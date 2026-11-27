package com.tychewealth.repository;

import java.time.LocalDateTime;
import java.util.List;

/** Performs batched as-of reads without materializing intervening checkpoints. */
public interface MarketPriceCheckpointAsOfRepository {

  List<MarketPriceCheckpointAtInstantProjection> findLatestBySymbolsAtInstants(
      List<String> symbols, List<LocalDateTime> capturedAts);
}
