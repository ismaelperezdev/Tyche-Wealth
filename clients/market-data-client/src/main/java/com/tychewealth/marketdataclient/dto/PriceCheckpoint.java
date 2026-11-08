package com.tychewealth.marketdataclient.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** A persisted market-price checkpoint. */
public record PriceCheckpoint(
    String symbol, BigDecimal price, Instant capturedAt, Instant bucketStart) {}
