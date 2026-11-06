package com.charter26.fix_initiator_client.normalizer;

import com.charter26.fix_initiator_client.model.NormalizedFxTick;

import org.springframework.stereotype.Component;

import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.NoMDEntries;
import quickfix.field.Symbol;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class FixMarketDataNormalizer {



    public NormalizedFxTick normalize(
            MarketDataSnapshotFullRefresh message,
            SessionID sessionId) throws FieldNotFound {

        // In our Initiator, TargetCompID identifies the LP.
        String lp = sessionId.getTargetCompID();

        String symbol = message.getString(Symbol.FIELD);

        BigDecimal bid = null;
        BigDecimal ask = null;
        BigDecimal bidSize = null;
        BigDecimal askSize = null;

        int entryCount = message.getInt(NoMDEntries.FIELD);

        for (int i = 1; i <= entryCount; i++) {

            MarketDataSnapshotFullRefresh.NoMDEntries group =
                    new MarketDataSnapshotFullRefresh.NoMDEntries();

            message.getGroup(i, group);

            char entryType = group.getChar(MDEntryType.FIELD);

            if (entryType == MDEntryType.BID) {

                bid = new BigDecimal(
                        group.getString(MDEntryPx.FIELD));

                if (group.isSetField(MDEntrySize.FIELD)) {
                    bidSize = new BigDecimal(
                            group.getString(MDEntrySize.FIELD));
                }

            } else if (entryType == MDEntryType.OFFER) {

                ask = new BigDecimal(
                        group.getString(MDEntryPx.FIELD));

                if (group.isSetField(MDEntrySize.FIELD)) {
                    askSize = new BigDecimal(
                            group.getString(MDEntrySize.FIELD));
                }
            }
        }

        // Our initial contract requires a complete bid/ask snapshot.
        if (bid == null || ask == null) {
            throw new IllegalArgumentException(
                    "Incomplete market data snapshot for " + symbol);
        }

        return new NormalizedFxTick(
                lp,
                symbol,
                bid,
                ask,
                bidSize,
                askSize,
                Instant.now()
        );
    }
}
