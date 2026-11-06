package com.charter26.fix_initiator_client.fix;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.charter26.fix_initiator_client.model.NormalizedFxTick;
import com.charter26.fix_initiator_client.normalizer.FixMarketDataNormalizer;

import quickfix.*;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;



@Component
public class FixClientApplication extends ApplicationAdapter {

    private final FixMarketDataNormalizer normalizer;

    public FixClientApplication(
            FixMarketDataNormalizer normalizer) {
        this.normalizer = normalizer;

    }
    private static final Logger log =
            LoggerFactory.getLogger(FixClientApplication.class);

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("Session Created: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("LOGON SUCCESS: {}", sessionId);
        try {
        subscribe(sessionId, "EUR/USD");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {
        log.info("LOGOUT: {}", sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.info("Sending Admin Message: {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat,
                   IncorrectTagValue, RejectLogon {

        log.info("Received Admin Message: {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat,
                   IncorrectTagValue, UnsupportedMessageType {

            if (message instanceof MarketDataSnapshotFullRefresh snapshot) {

                NormalizedFxTick tick =
                        normalizer.normalize(snapshot, sessionId);

                log.info("Normalized FX Tick: {}", tick);
            }
    }
    @Override
    public void toApp(Message message, SessionID sessionId)
            throws DoNotSend {

        log.info("Sending FIX Application Message: {}",
                message.toString().replace('\u0001', '|'));
    }    

    public void subscribe(SessionID sessionId, String symbol)throws SessionNotFound 
    {

        MarketDataRequest request = new MarketDataRequest(
                new MDReqID(UUID.randomUUID().toString()),
                new SubscriptionRequestType('1'),
                new MarketDepth(1)
        );

        request.set(new MDUpdateType(0));

        // BID
        MarketDataRequest.NoMDEntryTypes bid =
                new MarketDataRequest.NoMDEntryTypes();
        bid.set(new MDEntryType('0'));
        request.addGroup(bid);

        // ASK
        MarketDataRequest.NoMDEntryTypes ask =
                new MarketDataRequest.NoMDEntryTypes();
        ask.set(new MDEntryType('1'));
        request.addGroup(ask);

        // INSTRUMENT - REQUIRED
        MarketDataRequest.NoRelatedSym instrument =
                new MarketDataRequest.NoRelatedSym();

        instrument.set(new Symbol(symbol));
        request.addGroup(instrument);

        // Send after all groups are populated
        Session.sendToTarget(request, sessionId);
    }
}

