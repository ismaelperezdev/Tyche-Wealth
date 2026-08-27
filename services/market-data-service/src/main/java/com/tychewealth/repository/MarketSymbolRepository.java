package com.tychewealth.repository;

import com.tychewealth.entity.MarketSymbolEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Provides persistence operations for the market-symbol catalog. */
@Repository
public interface MarketSymbolRepository extends JpaRepository<MarketSymbolEntity, Long> {

  /** Inserts a batch of symbols that are not already present. */
  @Modifying
  @Query(
      value =
          "INSERT INTO market_symbols (symbol, active, deactivated_at) "
              + "SELECT unnest(CAST(:symbols AS text[])), :active, :deactivatedAt "
              + "ON CONFLICT (symbol) DO NOTHING",
      nativeQuery = true)
  void insertIfAbsentBatch(
      @Param("symbols") String[] symbols,
      @Param("active") boolean active,
      @Param("deactivatedAt") LocalDateTime deactivatedAt);

  /** Updates the state of a batch while preserving already completed transitions. */
  @Modifying
  @Query(
      value =
          "UPDATE market_symbols SET active = :active, deactivated_at = :deactivatedAt, "
              + "updated_at = CURRENT_TIMESTAMP WHERE symbol IN (:symbols) AND "
              + "(:active = FALSE AND (active = TRUE OR deactivated_at IS NULL) OR "
              + ":active = TRUE AND (active = FALSE OR deactivated_at IS NOT NULL))",
      nativeQuery = true)
  void updateActiveStateBatch(
      @Param("symbols") Collection<String> symbols,
      @Param("active") boolean active,
      @Param("deactivatedAt") LocalDateTime deactivatedAt);
}
