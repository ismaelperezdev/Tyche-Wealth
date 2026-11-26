package com.tychewealth.repository;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.model.MarketPriceCheckpointBatch;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Provides persistence operations for immutable market-price checkpoints. */
@Repository
public interface MarketPriceCheckpointRepository
    extends JpaRepository<MarketPriceCheckpointEntity, Long>,
        JpaSpecificationExecutor<MarketPriceCheckpointEntity> {

  @Override
  @EntityGraph(attributePaths = "marketSymbol")
  Page<MarketPriceCheckpointEntity> findAll(
      Specification<MarketPriceCheckpointEntity> specification, Pageable pageable);

  Slice<MarketPriceCheckpointEntity> findScanSlice(
      Specification<MarketPriceCheckpointEntity> specification, Pageable pageable);

  @EntityGraph(attributePaths = "marketSymbol")
  @Query(
      """
      select checkpoint
      from MarketPriceCheckpointEntity checkpoint
      where checkpoint.marketSymbol.symbol in :symbols
        and checkpoint.capturedAt <= :capturedAt
        and not exists (
          select newer.id
          from MarketPriceCheckpointEntity newer
          where newer.marketSymbol.id = checkpoint.marketSymbol.id
            and newer.capturedAt <= :capturedAt
            and (newer.capturedAt > checkpoint.capturedAt
              or (newer.capturedAt = checkpoint.capturedAt and newer.id > checkpoint.id))
        )
      """)
  List<MarketPriceCheckpointEntity> findLatestBySymbolsAtOrBefore(
      @Param("symbols") List<String> symbols, @Param("capturedAt") LocalDateTime capturedAt);

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
