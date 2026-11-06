package com.charter26.fix_accepter_server.marketdata.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(
        Map<String, InstrumentConfig> instruments,
        Map<String, ProviderConfig> providers
) {
    public record InstrumentConfig(
            String symbol,
            String type,
            int priceScale,
            BigDecimal pipSize
    ) {}

    public record ProviderConfig(
            Map<String, PricingConfig> instruments
    ) {}

    public record PricingConfig(
            BigDecimal initialMid,
            BigDecimal spreadPips,
            BigDecimal volatilityPips,
            long intervalMs,
            BigDecimal bidSize,
            BigDecimal askSize
    ) {}
}
