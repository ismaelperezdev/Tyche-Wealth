package com.tychewealth.marketdataclient.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** A persisted market-price checkpoint. */
public record PriceCheckpoint(
    String symbol,
    BigDecimal price,
    Instant capturedAt,
    Instant bucketStart,
    Instant requestedCapturedAt) {

  /** Creates a checkpoint without an as-of association, as in scalar history responses. */
  public PriceCheckpoint(String symbol, BigDecimal price, Instant capturedAt, Instant bucketStart) {
    this(symbol, price, capturedAt, bucketStart, null);
  }
}
