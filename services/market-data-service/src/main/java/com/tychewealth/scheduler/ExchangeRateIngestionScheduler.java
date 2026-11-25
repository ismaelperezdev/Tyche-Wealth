package com.tychewealth.scheduler;

import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_CLEANUP_FAILURE;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_CLEANUP_SUCCESS;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_INGESTION_ACTION;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_INGESTION_FAILURE;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_INGESTION_SCHEDULER;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_INGESTION_SUCCESS;

import com.tychewealth.service.ExchangeRateIngestionService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers periodic ingestion of Frankfurter exchange rates. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "app.exchange-rate-ingestion.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ExchangeRateIngestionScheduler {

  private static final long RATE_RETENTION_MONTHS = 2;

  private final ExchangeRateIngestionService exchangeRateIngestionService;

  @Scheduled(
      fixedDelayString = "${app.exchange-rate-ingestion.scheduler.fixed-delay:24h}",
      initialDelayString = "${app.exchange-rate-ingestion.scheduler.initial-delay:30s}")
  public void ingestLatestRates() {
    LocalDate executionDate = LocalDate.now();
    long start = System.currentTimeMillis();

    try {
      int savedRates = exchangeRateIngestionService.ingestLatestRates();
      log.info(
          EXCHANGE_RATE_INGESTION_SUCCESS,
          EXCHANGE_RATE_INGESTION_SCHEDULER,
          EXCHANGE_RATE_INGESTION_ACTION,
          savedRates,
          System.currentTimeMillis() - start);
    } catch (RuntimeException error) {
      log.error(
          EXCHANGE_RATE_INGESTION_FAILURE,
          EXCHANGE_RATE_INGESTION_SCHEDULER,
          EXCHANGE_RATE_INGESTION_ACTION,
          error);
      return;
    }

    try {
      int deletedRates =
          exchangeRateIngestionService.deleteRatesOlderThan(
              executionDate.minusMonths(RATE_RETENTION_MONTHS));
      log.info(
          EXCHANGE_RATE_CLEANUP_SUCCESS,
          EXCHANGE_RATE_INGESTION_SCHEDULER,
          EXCHANGE_RATE_INGESTION_ACTION,
          deletedRates);
    } catch (RuntimeException error) {
      log.error(
          EXCHANGE_RATE_CLEANUP_FAILURE,
          EXCHANGE_RATE_INGESTION_SCHEDULER,
          EXCHANGE_RATE_INGESTION_ACTION,
          error);
    }
  }
}
