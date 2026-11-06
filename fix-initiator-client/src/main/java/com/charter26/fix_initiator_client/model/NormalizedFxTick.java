package com.charter26.fix_initiator_client.model;

import java.math.BigDecimal;
import java.time.Instant;

public record NormalizedFxTick(
        String liquidityProvider,
        String symbol,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal bidSize,
        BigDecimal askSize,
        Instant receivedAt
) {}
