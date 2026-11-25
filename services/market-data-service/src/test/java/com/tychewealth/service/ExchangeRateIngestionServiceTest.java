package com.tychewealth.service;

import static com.tychewealth.constants.TestConstants.EUR_CURRENCY;
import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.TEST_EXCHANGE_RATE;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static com.tychewealth.testdata.ExchangeRateTestHelper.createIngestionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.client.FrankfurterApiClient;
import com.tychewealth.client.dto.FrankfurterRate;
import com.tychewealth.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ExchangeRateIngestionServiceTest {

  @Mock private FrankfurterApiClient frankfurterApiClient;
  @Mock private ExchangeRateRepository exchangeRateRepository;

  @Test
  void fetchesConfiguredRatesAndUpsertsTheReturnedObservations() {
    LocalDate effectiveDate = LocalDate.parse(EXCHANGE_RATE_TEST_DATE);
    List<FrankfurterRate> rates =
        List.of(
            new FrankfurterRate(
                effectiveDate, EUR_CURRENCY, USD_CURRENCY, new BigDecimal(TEST_EXCHANGE_RATE)),
            new FrankfurterRate(effectiveDate, EUR_CURRENCY, GBP_CURRENCY, new BigDecimal("0.85")));
    ExchangeRateIngestionService service =
        createIngestionService(frankfurterApiClient, exchangeRateRepository);
    when(frankfurterApiClient.fetchLatestRates(EUR_CURRENCY, List.of(USD_CURRENCY, GBP_CURRENCY)))
        .thenReturn(Mono.just(rates));
    when(exchangeRateRepository.upsertBatch(any(), any(), any(), any())).thenReturn(2);

    int savedCount = service.ingestLatestRates();

    assertThat(savedCount).isEqualTo(2);
    ArgumentCaptor<String[]> baseCurrencies = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<String[]> quoteCurrencies = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<String[]> effectiveDates = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<String[]> persistedRates = ArgumentCaptor.forClass(String[].class);
    verify(exchangeRateRepository)
        .upsertBatch(
            baseCurrencies.capture(),
            quoteCurrencies.capture(),
            effectiveDates.capture(),
            persistedRates.capture());
    assertThat(baseCurrencies.getValue()).containsExactly(EUR_CURRENCY, EUR_CURRENCY);
    assertThat(quoteCurrencies.getValue()).containsExactly(USD_CURRENCY, GBP_CURRENCY);
    assertThat(effectiveDates.getValue())
        .containsExactly(EXCHANGE_RATE_TEST_DATE, EXCHANGE_RATE_TEST_DATE);
    assertThat(persistedRates.getValue()).containsExactly(TEST_EXCHANGE_RATE, "0.85");
  }

  @Test
  void returnsZeroAndSkipsPersistenceWhenProviderReturnsNoRates() {
    ExchangeRateIngestionService service =
        createIngestionService(frankfurterApiClient, exchangeRateRepository);
    when(frankfurterApiClient.fetchLatestRates(EUR_CURRENCY, List.of(USD_CURRENCY, GBP_CURRENCY)))
        .thenReturn(Mono.just(List.of()));

    int savedCount = service.ingestLatestRates();

    assertThat(savedCount).isZero();
    verifyNoInteractions(exchangeRateRepository);
  }

  @Test
  void propagatesProviderFailureWithoutPersistingRates() {
    ExchangeRateIngestionService service =
        createIngestionService(frankfurterApiClient, exchangeRateRepository);
    IllegalStateException providerFailure = new IllegalStateException("Frankfurter unavailable");
    when(frankfurterApiClient.fetchLatestRates(EUR_CURRENCY, List.of(USD_CURRENCY, GBP_CURRENCY)))
        .thenReturn(Mono.error(providerFailure));

    assertThatThrownBy(service::ingestLatestRates)
        .isSameAs(providerFailure)
        .hasMessage("Frankfurter unavailable");
    verifyNoInteractions(exchangeRateRepository);
  }

  @Test
  void deletesRatesOlderThanTheSuppliedCutoffDate() {
    LocalDate cutoffDate = LocalDate.parse(EXCHANGE_RATE_TEST_DATE);
    when(exchangeRateRepository.deleteRatesOlderThan(cutoffDate)).thenReturn(3);

    int deletedCount =
        createIngestionService(frankfurterApiClient, exchangeRateRepository)
            .deleteRatesOlderThan(cutoffDate);

    assertThat(deletedCount).isEqualTo(3);
    verify(exchangeRateRepository).deleteRatesOlderThan(cutoffDate);
  }
}
