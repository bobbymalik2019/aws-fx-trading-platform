package com.charter26.fix_accepter_server.marketdata.subscription;

import org.springframework.stereotype.Component;

import com.charter26.fix_accepter_server.marketdata.model.MarketDataSubscription;

import quickfix.SessionID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class MarketDataSubscriptionRegistry {

    private final Map<SessionID, Map<String, Set<String>>>
            subscriptions = new ConcurrentHashMap<>();

    public void subscribe(
            SessionID sessionId,
            String requestId,
            Set<String> instruments) {

        subscriptions
            .computeIfAbsent(
                sessionId,
                id -> new ConcurrentHashMap<>())
            .put(requestId, Set.copyOf(instruments));
    }

    public void unsubscribe(
            SessionID sessionId,
            String requestId) {

        Map<String, Set<String>> requests =
                subscriptions.get(sessionId);

        if (requests != null) {
            requests.remove(requestId);

            if (requests.isEmpty()) {
                subscriptions.remove(sessionId, requests);
            }
        }
    }

    public boolean isSubscribed(
            SessionID sessionId,
            String instrumentId) {

        Map<String, Set<String>> requests =
                subscriptions.get(sessionId);

        return requests != null &&
                requests.values().stream()
                    .anyMatch(set -> set.contains(instrumentId));
    }

    public void removeSession(SessionID sessionId) {
        subscriptions.remove(sessionId);
    }



    public List<MarketDataSubscription> getActiveSubscriptions() 
    {

        List<MarketDataSubscription> result = new ArrayList<>();

        subscriptions.forEach((sessionId, requests) -> {

            requests.forEach((requestId, instrumentIds) -> {

                instrumentIds.forEach(instrumentId -> {

                    result.add(new MarketDataSubscription(
                            sessionId,
                            requestId,
                            instrumentId
                    ));

                });
            });
        });

        return result;
}
}