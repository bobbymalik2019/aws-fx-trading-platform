package com.charter26.fix_accepter_server.marketdata.service;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.charter26.fix_accepter_server.marketdata.model.PriceTick;
import com.charter26.fix_accepter_server.marketdata.subscription.MarketDataSubscriptionRegistry;
import com.charter26.fix_accepter_server.marketdata.model.PriceTick;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

@Service
public class FixMarketDataPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(FixMarketDataPublisher.class);

    private final MarketDataSubscriptionRegistry subscriptions;

    public FixMarketDataPublisher(
            MarketDataSubscriptionRegistry subscriptions) {
        this.subscriptions = subscriptions;
    }

    // public void publish(PriceTick tick) {

    //     subscriptions.getActiveSubscriptions().stream()
    //         .filter(sub -> sub.instrumentId().equals(tick.instrumentId()))
    //         .filter(sub -> sub.sessionId().getSenderCompID()
    //                 .equals(tick.lp()))
    //         .forEach(sub ->
    //                 sendSnapshot(
    //                         sub.sessionId(),
    //                         sub.requestId(),
    //                         tick.symbol(),
    //                         tick));
    // }

public void publish(PriceTick tick) {

    var activeSubscriptions =
            subscriptions.getActiveSubscriptions();

    log.info(
        "PUBLISH REQUEST: LP={}, Instrument={}, ActiveSubscriptions={}",
        tick.lp(),
        tick.instrumentId(),
        activeSubscriptions.size()
    );

    activeSubscriptions.stream()
        .filter(sub -> {

            boolean instrumentMatches =
                    sub.instrumentId().equals(tick.instrumentId());

            boolean lpMatches =
                    sub.sessionId().getSenderCompID().equals(tick.lp());

            log.info(
                "SUBSCRIPTION CHECK: Session={}, MDReqID={}, InstrumentMatch={}, LPMatch={}",
                sub.sessionId(),
                sub.requestId(),
                instrumentMatches,
                lpMatches
            );

            return instrumentMatches && lpMatches;
        })
        .forEach(sub -> {

            log.info(
                "MATCH FOUND: LP={}, Instrument={}, Session={}",
                tick.lp(),
                tick.instrumentId(),
                sub.sessionId()
            );

            sendSnapshot(
                    sub.sessionId(),
                    sub.requestId(),
                    tick.symbol(),
                    tick
            );
        });
}    

    private void sendSnapshot(
            SessionID sessionId,
            String requestId,
            String symbol,
            PriceTick tick) {

        Session session = Session.lookupSession(sessionId);

        if (session == null || !session.isLoggedOn()) {
            return;
        }

        MarketDataSnapshotFullRefresh snapshot =
                new MarketDataSnapshotFullRefresh();

        snapshot.set(new MDReqID(requestId));
        snapshot.set(new Symbol(symbol));

        MarketDataSnapshotFullRefresh.NoMDEntries bid =
                new MarketDataSnapshotFullRefresh.NoMDEntries();

        bid.set(new MDEntryType(MDEntryType.BID));
        bid.set(new MDEntryPx(tick.bid().doubleValue()));
        bid.set(new MDEntrySize(tick.bidSize().doubleValue()));
        snapshot.addGroup(bid);

        MarketDataSnapshotFullRefresh.NoMDEntries ask =
                new MarketDataSnapshotFullRefresh.NoMDEntries();

        ask.set(new MDEntryType(MDEntryType.OFFER));
        ask.set(new MDEntryPx(tick.ask().doubleValue()));
        ask.set(new MDEntrySize(tick.askSize().doubleValue()));
        snapshot.addGroup(ask);

        try {
            Session.sendToTarget(snapshot, sessionId);

            log.info(
                    "Published tick: LP={} Symbol={} Bid={} Ask={} MDReqID={}",
                    sessionId.getSenderCompID(),
                    symbol,
                    tick.bid(),
                    tick.ask(),
                    requestId);

        } catch (SessionNotFound e) {
            log.error("FIX session unavailable: {}", sessionId, e);
        }
    }
}

