package com.tychewealth.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tychewealth.service.ExchangeRateIngestionService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateIngestionSchedulerTest {

  @Mock private ExchangeRateIngestionService exchangeRateIngestionService;

  private ExchangeRateIngestionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new ExchangeRateIngestionScheduler(exchangeRateIngestionService);
  }

  @Test
  void ingestsRatesThenDeletesObservationsOlderThanTwoMonths() {
    LocalDate earliestExpectedCutoff = LocalDate.now().minusMonths(2);

    scheduler.ingestLatestRates();

    ArgumentCaptor<LocalDate> cutoffDate = ArgumentCaptor.forClass(LocalDate.class);
    InOrder inOrder = inOrder(exchangeRateIngestionService);
    inOrder.verify(exchangeRateIngestionService).ingestLatestRates();
    inOrder.verify(exchangeRateIngestionService).deleteRatesOlderThan(cutoffDate.capture());
    assertThat(cutoffDate.getValue()).isEqualTo(earliestExpectedCutoff);
  }

  @Test
  void skipsCleanupWhenIngestionFails() {
    doThrow(new IllegalStateException("Frankfurter unavailable"))
        .when(exchangeRateIngestionService)
        .ingestLatestRates();

    scheduler.ingestLatestRates();

    verify(exchangeRateIngestionService).ingestLatestRates();
    verifyNoMoreInteractions(exchangeRateIngestionService);
  }
}
