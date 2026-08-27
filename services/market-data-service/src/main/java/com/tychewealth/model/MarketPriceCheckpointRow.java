package com.tychewealth.model;

/** Database-ready values for a single market-price checkpoint. */
public record MarketPriceCheckpointRow(
    String marketSymbolId, String price, String capturedAt, String bucketStart) {}
