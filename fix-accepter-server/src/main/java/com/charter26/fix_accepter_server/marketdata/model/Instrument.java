package com.charter26.fix_accepter_server.marketdata.model;

import java.math.BigDecimal;

public record Instrument(
        String instrumentId,
        String symbol,
        InstrumentType type,
        int priceScale,
        BigDecimal pipSize
) {}

