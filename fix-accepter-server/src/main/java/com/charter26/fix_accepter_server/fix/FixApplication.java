
package com.charter26.fix_accepter_server.fix;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.charter26.fix_accepter_server.marketdata.model.PriceTick;
import com.charter26.fix_accepter_server.marketdata.registry.InstrumentRegistry;
import com.charter26.fix_accepter_server.marketdata.service.MarketDataSimulator;
import com.charter26.fix_accepter_server.marketdata.subscription.MarketDataSubscriptionRegistry;

import quickfix.*;

//import java.math.BigDecimal;
// import java.util.HashSet;
// import java.util.Set;

import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataRequestReject;
import quickfix.fix44.MarketDataSnapshotFullRefresh;



@Component
public class FixApplication extends ApplicationAdapter {

    private static final Logger log =
            LoggerFactory.getLogger(FixApplication.class);

    private final MarketDataSubscriptionRegistry subscriptions;
    private final InstrumentRegistry instruments;
    private final MarketDataSimulator marketDataSimulator;

    public FixApplication(
            MarketDataSubscriptionRegistry subscriptions,
            InstrumentRegistry instruments,
            MarketDataSimulator marketDataSimulator) {

        this.subscriptions = subscriptions;
        this.instruments = instruments;
        this.marketDataSimulator = marketDataSimulator;
    }


    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat,
                IncorrectTagValue, RejectLogon {

        log.info("FROM ADMIN [{}]: {}", sessionId, message);
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("Session Created: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("LOGON SUCCESS: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
         subscriptions.removeSession(sessionId);
        log.info("FIX Session logged out. Subscriptions ,LOGOUT: {}", sessionId);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat,
                   IncorrectTagValue, UnsupportedMessageType {

        log.info("Message received from CLIENT: {}", message);

        String lp = sessionId.getSenderCompID();

        log.info("LP Session: {}", lp);

            if (message instanceof quickfix.fix44.MarketDataRequest request) {
                handleMarketDataRequest(request, sessionId);
                }
    }

    private void handleMarketDataRequest(quickfix.fix44.MarketDataRequest request,SessionID sessionId) throws FieldNotFound 
    {

        // String requestId = request.getMDReqID().getValue();
        // char subscriptionType = request.getSubscriptionRequestType().getValue();
        
        // log.info("MarketDataRequest: LP={}, MDReqID={}, Type={}",sessionId.getSenderCompID(),requestId,subscriptionType);

        // // 1. Validate subscription type
        // if (subscriptionType != '0' && subscriptionType != '1' && subscriptionType != '2') {
        //     throw new IllegalArgumentException(
        //             "Unsupported subscription type: " + subscriptionType);
        // }

        // // 2. Handle cancellation using original MDReqID
        // if (subscriptionType == '2') {

        //     subscriptions.unsubscribe(sessionId, requestId);
        //     log.info("UNSUBSCRIBED: Session={}, MDReqID={}",sessionId, requestId);
        //     return;
        // }

        // // 3. We support only top-of-book
        // int depth = request.getMarketDepth().getValue();

        // if (depth != 1) {
        //     rejectMarketDataRequest(
        //             sessionId, requestId, '5',
        //             "Only MarketDepth=1 is supported");
        //     return;
        // }

        // // 4. We support full refresh, not incremental
        // if (request.isSetMDUpdateType()
        //         && request.getMDUpdateType().getValue() != 0) {

        //     rejectMarketDataRequest(
        //             sessionId, requestId, '6',
        //             "Only full refresh updates are supported");
        //     return;
        // }        

        // // Extract requested instruments
        // Set<String> instrumentIds = new HashSet<>();

        // int count = request.getNoRelatedSym().getValue();

        // for (int i = 1; i <= count; i++) {
        //     quickfix.fix44.MarketDataRequest.NoRelatedSym group =
        //             new quickfix.fix44.MarketDataRequest.NoRelatedSym();

        //     request.getGroup(i, group);

        //     String symbol = group.getSymbol().getValue();

        //     // Resolve FIX symbol to internal instrument ID
        //     instruments.getAll().values().stream()
        //             .filter(inst -> inst.symbol().equals(symbol))
        //             .findFirst()
        //             .ifPresent(inst ->
        //                     instrumentIds.add(inst.instrumentId()));
        // }

        // if (instrumentIds.isEmpty()) {
        //     throw new IllegalArgumentException(
        //             "No valid instruments in request " + requestId);
        // }

        // if (subscriptionType == '1') {
        //     subscriptions.subscribe(
        //             sessionId, requestId, instrumentIds);

        //     System.out.println(
        //             "MARKET DATA SUBSCRIBED: " +
        //             sessionId + " " + instrumentIds);
        // } else {
        //     subscriptions.unsubscribe(sessionId, requestId);

        //     System.out.println(
        //             "MARKET DATA UNSUBSCRIBED: " +
        //             sessionId + " " + requestId);
        // }

        String requestId = request.getMDReqID().getValue();
        char subscriptionType =
                request.getSubscriptionRequestType().getValue();

        log.info("MarketDataRequest: LP={}, MDReqID={}, Type={}",
                sessionId.getSenderCompID(),
                requestId,
                subscriptionType);

        // 1. Validate subscription type
        if (subscriptionType != '0'
                && subscriptionType != '1'
                && subscriptionType != '2') {

            rejectMarketDataRequest(
                    sessionId, requestId, '4',
                    "Unsupported subscription type");
            return;
        }

        // 2. Handle cancellation using original MDReqID
        if (subscriptionType == '2') {

            subscriptions.unsubscribe(sessionId, requestId);

            log.info("UNSUBSCRIBED: Session={}, MDReqID={}",
                    sessionId, requestId);
            return;
        }

        // 3. We support only top-of-book
        int depth = request.getMarketDepth().getValue();

        if (depth != 1) {
            rejectMarketDataRequest(
                    sessionId, requestId, '5',
                    "Only MarketDepth=1 is supported");
            return;
        }

        // 4. We support full refresh, not incremental
        if (request.isSetMDUpdateType()
                && request.getMDUpdateType().getValue() != 0) {

            rejectMarketDataRequest(
                    sessionId, requestId, '6',
                    "Only full refresh updates are supported");
            return;
        }

        // 5. Extract requested market data entry types
        Set<Character> entryTypes = new HashSet<>();

        int entryCount = request.getNoMDEntryTypes().getValue();

        for (int i = 1; i <= entryCount; i++) {

            MarketDataRequest.NoMDEntryTypes group =
                    new MarketDataRequest.NoMDEntryTypes();

            request.getGroup(i, group);

            entryTypes.add(group.getMDEntryType().getValue());
        }

        // Demo requires both bid and offer, and nothing else
        if (entryTypes.size() != 2
                || !entryTypes.contains('0')
                || !entryTypes.contains('1')) {

            rejectMarketDataRequest(
                    sessionId, requestId, '8',
                    "Only bid and offer entry types are supported");
            return;
        }

        // 6. Resolve requested symbols to instrument IDs
        Set<String> instrumentIds = new HashSet<>();
        Set<String> symbols = new HashSet<>();

        int count = request.getNoRelatedSym().getValue();

        for (int i = 1; i <= count; i++) {

            MarketDataRequest.NoRelatedSym group =
                    new MarketDataRequest.NoRelatedSym();

            request.getGroup(i, group);

            String symbol = group.getSymbol().getValue();

            var instrument = instruments.getAll()
                    .values()
                    .stream()
                    .filter(inst -> inst.symbol().equals(symbol))
                    .findFirst();

            if (instrument.isEmpty()) {
                rejectMarketDataRequest(
                        sessionId, requestId, '0',
                        "Unknown instrument: " + symbol);
                return;
            }

            instrumentIds.add(instrument.get().instrumentId());
            symbols.add(symbol);
        }

        if (instrumentIds.isEmpty()) {
            rejectMarketDataRequest(
                    sessionId, requestId, '0',
                    "No valid instruments requested");
            return;
        }

        // 7. Snapshot only: no permanent subscription
        if (subscriptionType == '0') {

            log.info("SNAPSHOT ONLY: Session={}, Symbols={}",
                    sessionId, symbols);

            for (String symbol : symbols) {
                PriceTick tick = marketDataSimulator.getLatestTick(
                        sessionId.getSenderCompID(), symbol);
                if (tick == null) {
                                log.warn("No tick available: LP={}, Instrument={}",
                    sessionId.getSenderCompID(), symbol);
                    continue;
                }
                sendInitialSnapshot(sessionId, requestId, tick);
            }
            return;
        }

        // 8. Streaming subscription
        subscriptions.subscribe(
                sessionId, requestId, instrumentIds);

        log.info("ACTIVE SUBSCRIPTIONS:");
        subscriptions.getActiveSubscriptions()
            .forEach(subscription ->
                log.info("{}", subscription));

        log.info("SUBSCRIBED: Session={}, MDReqID={}, Instruments={}",
                sessionId, requestId, instrumentIds);

        // Send initial market data
        for (String symbol : symbols) {
            PriceTick tick = marketDataSimulator.getLatestTick(
                    sessionId.getSenderCompID(), symbol);
            if (tick == null) {
            log.warn("No tick available for LP={} Instrument={}",
                    sessionId.getSenderCompID(), symbol);
            continue;
            }
            sendInitialSnapshot(sessionId, requestId, tick);
        }
    }




    private void rejectMarketDataRequest(SessionID sessionId,String requestId,char reason,String description) 
    {

        MarketDataRequestReject reject =new MarketDataRequestReject(new MDReqID(requestId));

        reject.set(new MDReqRejReason(reason));
        reject.set(new quickfix.field.Text(description));

        try {
            Session.sendToTarget(reject, sessionId);

            log.warn( "MARKET DATA REJECTED: Session={}, RequestId={}, Reason={}", sessionId, requestId, description);

        } catch (SessionNotFound e) {
            log.error(
                "Unable to send rejection to {}",
                sessionId, e);
        }
    }

    
    private void sendInitialSnapshot(SessionID sessionId,String requestId,PriceTick tick) {

        MarketDataSnapshotFullRefresh snapshot =new MarketDataSnapshotFullRefresh();

        snapshot.set(new MDReqID(requestId));
        snapshot.set(new Symbol(tick.symbol()));

        // Demo prices only
        //PriceTick tick = marketDataSimulator.getLatestTick(sessionId.getSenderCompID(), instrumentId);
        //double bid = 1.08500;
        //double ask = 1.08520;

        // BID
        MarketDataSnapshotFullRefresh.NoMDEntries bidEntry = new MarketDataSnapshotFullRefresh.NoMDEntries();
        // bidEntry.set(new MDEntryPx(bid));
        // bidEntry.set(new MDEntrySize(1000000.0));
      
        // snapshot.addGroup(bidEntry);

        bidEntry.set(new MDEntryType(MDEntryType.BID));
        bidEntry.set(new MDEntryPx(tick.bid().doubleValue()));
        bidEntry.set(new MDEntrySize(1000000.0));

        snapshot.addGroup(bidEntry);

        // OFFER
        MarketDataSnapshotFullRefresh.NoMDEntries askEntry =new MarketDataSnapshotFullRefresh.NoMDEntries();

        askEntry.set(new MDEntryType(MDEntryType.OFFER));
        askEntry.set(new MDEntryPx(tick.ask().doubleValue()));
        askEntry.set(new MDEntrySize(1000000.0));

        snapshot.addGroup(askEntry);

        try {
            log.info("OUTGOING SNAPSHOT: {}",snapshot.toString().replace('\u0001', '|'));
            Session.sendToTarget(snapshot, sessionId);

            log.info(
                "SNAPSHOT SENT: Session={}, Symbol={}, Bid={}, Ask={}",
                sessionId, tick.symbol(), tick.bid().doubleValue(), tick.ask().doubleValue() );

        } catch (SessionNotFound e) {
            log.error("Unable to send snapshot", e);
        }
    }


    public void subscribe(SessionID sessionId, String symbol) throws Exception 
    {

        quickfix.fix44.MarketDataRequest request =
                new quickfix.fix44.MarketDataRequest(
                    new quickfix.field.MDReqID(
                        java.util.UUID.randomUUID().toString()),
                    new quickfix.field.SubscriptionRequestType('1'),
                    new quickfix.field.MarketDepth(1)
                );

        request.set(new quickfix.field.MDUpdateType(0));

        // Request BID
        quickfix.fix44.MarketDataRequest.NoMDEntryTypes bid =
                new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();

        bid.set(new quickfix.field.MDEntryType('0'));
        request.addGroup(bid);

        // Request ASK
        quickfix.fix44.MarketDataRequest.NoMDEntryTypes ask =
                new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();

        ask.set(new quickfix.field.MDEntryType('1'));
        request.addGroup(ask);

        // Requested instrument
        quickfix.fix44.MarketDataRequest.NoRelatedSym instrument =
                new quickfix.fix44.MarketDataRequest.NoRelatedSym();

        instrument.set(new quickfix.field.Symbol(symbol));
        request.addGroup(instrument);

        quickfix.Session.sendToTarget(request, sessionId);
    }
}

