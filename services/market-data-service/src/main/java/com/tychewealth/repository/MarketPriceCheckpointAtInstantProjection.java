package com.tychewealth.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Checkpoint fields returned by the as-of batch query. */
public record MarketPriceCheckpointAtInstantProjection(
    String symbol,
    BigDecimal price,
    LocalDateTime capturedAt,
    LocalDateTime bucketStart,
    long requestedIndex) {}
