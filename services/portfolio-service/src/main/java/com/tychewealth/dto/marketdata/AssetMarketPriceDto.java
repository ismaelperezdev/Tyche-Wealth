package com.tychewealth.dto.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/** An asset together with its latest available market quote. */
public record AssetMarketPriceDto(
    Long assetId,
    Long portfolioId,
    String symbol,
    BigDecimal quantity,
    BigDecimal averagePrice,
    BigDecimal marketPrice,
    Instant capturedAt) {}
