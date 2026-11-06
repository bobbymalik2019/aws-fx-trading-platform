package com.charter26.fix_accepter_server.marketdata.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.charter26.fix_accepter_server.marketdata.config.MarketDataProperties.PricingConfig;
import com.charter26.fix_accepter_server.marketdata.model.Instrument;
import com.charter26.fix_accepter_server.marketdata.model.InstrumentType;
import com.charter26.fix_accepter_server.marketdata.model.PriceTick;

@Component
public class FxSpotTickGenerator {

    public PriceTick generate(
            String lp,
            Instrument instrument,
            PricingConfig config,
            PriceTick previousTick) {

        if (instrument.type() != InstrumentType.FX_SPOT) {
            throw new IllegalArgumentException(
                    "Unsupported instrument type: " + instrument.type());
        }

        BigDecimal mid = previousTick == null
                ? config.initialMid()
                : previousTick.bid()
                    .add(previousTick.ask())
                    .divide(BigDecimal.valueOf(2));

        double randomMovement =
                ThreadLocalRandom.current().nextDouble(-1.0, 1.0);

        BigDecimal movement = config.volatilityPips()
                .multiply(BigDecimal.valueOf(randomMovement))
                .multiply(instrument.pipSize());

        mid = mid.add(movement);

        BigDecimal halfSpread = config.spreadPips()
                .multiply(instrument.pipSize())
                .divide(BigDecimal.valueOf(2));

        BigDecimal bid = mid.subtract(halfSpread)
                .setScale(instrument.priceScale(), RoundingMode.HALF_UP);

        BigDecimal ask = mid.add(halfSpread)
                .setScale(instrument.priceScale(), RoundingMode.HALF_UP);

        return new PriceTick(
                lp,
                instrument.instrumentId(),
                instrument.symbol(),
                instrument.type(),
                bid,
                ask,
                config.bidSize(),
                config.askSize(),
                Instant.now()
        );
    }
}
