package com.charter26.fix_accepter_server.marketdata.service;

import java.util.Map;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import com.charter26.fix_accepter_server.marketdata.config.MarketDataProperties;
import com.charter26.fix_accepter_server.marketdata.engine.FxSpotTickGenerator;
import com.charter26.fix_accepter_server.marketdata.model.*;
import com.charter26.fix_accepter_server.marketdata.registry.InstrumentRegistry;

@Service
public class MarketDataSimulator implements SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(MarketDataSimulator.class);

    private final MarketDataProperties properties;
    private final InstrumentRegistry registry;
    private final FxSpotTickGenerator generator;
    private final FixMarketDataPublisher publisher;

    private final Map<String, PriceTick> latestTicks =
            new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private volatile boolean running;

    public MarketDataSimulator(
            MarketDataProperties properties,
            InstrumentRegistry registry,
            FxSpotTickGenerator generator,
            FixMarketDataPublisher publisher) {

        this.properties = properties;
        this.registry = registry;
        this.generator = generator;
        this.publisher = publisher;
    }

    @Override
    public synchronized void start() {

        if (running) return;

        int taskCount = properties.providers().values().stream()
                .mapToInt(p -> p.instruments().size())
                .sum();

        // Validate all configured instruments before scheduling.
        properties.providers().forEach((lp, provider) ->
                provider.instruments().forEach((id, config) -> {

                    Instrument instrument = registry.get(id);

                    if (instrument.type() != InstrumentType.FX_SPOT) {
                        throw new IllegalArgumentException(
                                "Only FX_SPOT is supported: " + id);
                    }

                    if (config.intervalMs() <= 0) {
                        throw new IllegalArgumentException(
                                "Invalid interval for " + lp + "/" + id);
                    }

                    if (config.initialMid() == null
                            || config.spreadPips() == null
                            || config.volatilityPips() == null
                            || config.bidSize() == null
                            || config.askSize() == null
                            || config.initialMid().signum() <= 0
                            || config.spreadPips().signum() < 0
                            || config.volatilityPips().signum() < 0
                            || config.bidSize().signum() <= 0
                            || config.askSize().signum() <= 0) {
                        throw new IllegalArgumentException(
                                "Invalid pricing configuration: " + lp + "/" + id);
                    }
                })
        );

        scheduler = Executors.newScheduledThreadPool(
                Math.max(1, Math.min(taskCount, 4))
        );

        try {
            properties.providers().forEach((lp, provider) -> {

                provider.instruments().forEach((id, config) -> {

                    Instrument instrument = registry.get(id);
                    String key = lp + "|" + id;

                    scheduler.scheduleWithFixedDelay(() -> {

                        try {
                            PriceTick previous = latestTicks.get(key);

                            PriceTick tick = generator.generate(
                                    lp, instrument, config, previous);

                            latestTicks.put(key, tick);
                            publisher.publish(tick);

                            log.info(
                                    "TICK LP={} INSTRUMENT={} BID={} ASK={} BID_SIZE={} ASK_SIZE={}",
                                    lp,
                                    id,
                                    tick.bid(),
                                    tick.ask(),
                                    tick.bidSize(),
                                    tick.askSize()
                            );

                        } catch (Exception e) {
                            log.error("Tick generation failed: {}", key, e);
                        }

                    }, 0, config.intervalMs(), TimeUnit.MILLISECONDS);
                });
            });

            running = true;
            log.info("Market Data Simulator started with {} tasks", taskCount);

        } catch (RuntimeException e) {
            scheduler.shutdownNow();
            scheduler = null;
            throw e;
        }
    }

    @Override
    public synchronized void stop() {

        running = false;

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        log.info("Market Data Simulator stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public PriceTick getLatestTick(String lp, String instrumentId) {
        return latestTicks.get(lp + "|" + instrumentId);
    }
}
