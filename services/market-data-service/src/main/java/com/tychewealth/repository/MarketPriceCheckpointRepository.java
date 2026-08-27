package com.tychewealth.repository;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.model.MarketPriceCheckpointBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Provides persistence operations for immutable market-price checkpoints. */
@Repository
public interface MarketPriceCheckpointRepository
    extends JpaRepository<MarketPriceCheckpointEntity, Long> {

  default void insertIfAbsentBatch(MarketPriceCheckpointBatch batch) {
    insertIfAbsentBatchNative(
        batch.marketSymbolIds().toArray(String[]::new),
        batch.prices().toArray(String[]::new),
        batch.capturedAts().toArray(String[]::new),
        batch.bucketStarts().toArray(String[]::new));
  }

  /** Inserts checkpoints atomically, ignoring an already captured symbol and bucket. */
  @Modifying
  @Query(
      value =
          "INSERT INTO market_price_checkpoints "
              + "(market_symbol_id, price, captured_at, bucket_start) "
              + "SELECT rows.market_symbol_id, rows.price, rows.captured_at, rows.bucket_start "
              + "FROM unnest(CAST(:marketSymbolIds AS bigint[]), "
              + "CAST(:prices AS numeric[]), CAST(:capturedAts AS timestamp[]), "
              + "CAST(:bucketStarts AS timestamp[])) "
              + "AS rows(market_symbol_id, price, captured_at, bucket_start) "
              + "ON CONFLICT (market_symbol_id, bucket_start) DO NOTHING",
      nativeQuery = true)
  void insertIfAbsentBatchNative(
      @Param("marketSymbolIds") String[] marketSymbolIds,
      @Param("prices") String[] prices,
      @Param("capturedAts") String[] capturedAts,
      @Param("bucketStarts") String[] bucketStarts);
}
