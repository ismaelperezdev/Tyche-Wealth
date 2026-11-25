package com.tychewealth.repository;

import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.entity.ExchangeRateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Provides persistence operations for exchange rates. */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, ExchangeRateId> {

  /** Inserts or updates a batch of rates, safely handling repeated ingestion of the same date. */
  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO exchange_rates (base_currency, quote_currency, effective_date, rate) "
              + "SELECT rows.base_currency, rows.quote_currency, rows.effective_date, rows.rate "
              + "FROM unnest(CAST(:baseCurrencies AS text[]), "
              + "CAST(:quoteCurrencies AS text[]), CAST(:effectiveDates AS date[]), "
              + "CAST(:rates AS numeric[])) "
              + "AS rows(base_currency, quote_currency, effective_date, rate) "
              + "ON CONFLICT (base_currency, quote_currency, effective_date) "
              + "DO UPDATE SET rate = EXCLUDED.rate",
      nativeQuery = true)
  int upsertBatch(
      @Param("baseCurrencies") String[] baseCurrencies,
      @Param("quoteCurrencies") String[] quoteCurrencies,
      @Param("effectiveDates") String[] effectiveDates,
      @Param("rates") String[] rates);
}
