package com.charter26.fix_accepter_server.marketdata.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.charter26.fix_accepter_server.marketdata.config.MarketDataProperties;
import com.charter26.fix_accepter_server.marketdata.model.Instrument;
import com.charter26.fix_accepter_server.marketdata.model.InstrumentType;
import com.charter26.fix_accepter_server.marketdata.subscription.MarketDataSubscriptionRegistry;

@Component
public class InstrumentRegistry {

    private final MarketDataSubscriptionRegistry subscriptions;
    private final Map<String, Instrument> instruments;

    public InstrumentRegistry(MarketDataSubscriptionRegistry subscriptions,MarketDataProperties properties) {

        this.subscriptions = subscriptions;
        Map<String, Instrument> registry = new HashMap<>();

        properties.instruments().forEach((id, config) -> {

            Instrument instrument = new Instrument(
                    id,
                    config.symbol(),
                    InstrumentType.valueOf(config.type()),
                    config.priceScale(),
                    config.pipSize()
            );

            registry.put(id, instrument);
        });

        this.instruments = Collections.unmodifiableMap(registry);
    }

    public Instrument get(String instrumentId) {

        Instrument instrument = instruments.get(instrumentId);

        if (instrument == null) {
            throw new IllegalArgumentException(
                    "Unknown instrument: " + instrumentId);
        }

        return instrument;
    }

    public Map<String, Instrument> getAll() {
        return instruments;
    }
}

