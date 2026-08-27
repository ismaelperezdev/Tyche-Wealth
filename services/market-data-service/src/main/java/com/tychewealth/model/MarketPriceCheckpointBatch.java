package com.tychewealth.model;

import java.util.List;

/** Batch-shaped values used to insert market-price checkpoints. */
public record MarketPriceCheckpointBatch(
    List<String> marketSymbolIds,
    List<String> prices,
    List<String> capturedAts,
    List<String> bucketStarts) {

  public MarketPriceCheckpointBatch {
    marketSymbolIds = List.copyOf(marketSymbolIds);
    prices = List.copyOf(prices);
    capturedAts = List.copyOf(capturedAts);
    bucketStarts = List.copyOf(bucketStarts);
  }
}
