package com.charter26.fix_accepter_server.marketdata.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceTick(
        String lp,
        String instrumentId,
        String symbol,
        InstrumentType instrumentType,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal bidSize,
        BigDecimal askSize,
        Instant timestamp
) {}
