package com.charter26.fix_accepter_server.marketdata.model;

import quickfix.SessionID;

public record MarketDataSubscription(
        SessionID sessionId,
        String requestId,
        String instrumentId) {
}
