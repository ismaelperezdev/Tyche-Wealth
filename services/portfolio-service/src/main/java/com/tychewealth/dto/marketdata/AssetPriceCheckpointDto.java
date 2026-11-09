package com.tychewealth.dto.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/** A market-price checkpoint associated with one of the user's assets. */
public record AssetPriceCheckpointDto(
    Long assetId,
    Long portfolioId,
    String symbol,
    BigDecimal price,
    Instant capturedAt,
    Instant bucketStart) {}
