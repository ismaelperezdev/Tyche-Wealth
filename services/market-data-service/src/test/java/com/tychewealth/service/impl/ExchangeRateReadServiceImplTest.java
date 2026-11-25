package com.tychewealth.service.impl;

import static com.tychewealth.constants.TestConstants.EUR_CURRENCY;
import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.entity.ExchangeRateId;
import com.tychewealth.error.exception.ExchangeRateDataInconsistencyException;
import com.tychewealth.mapper.ExchangeRateReadMapper;
import com.tychewealth.repository.ExchangeRateRepository;
import com.tychewealth.service.helper.ExchangeRateQueryValidationHelper;
import com.tychewealth.service.helper.ExchangeRateReadHelper;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateReadServiceImplTest {

  @Mock private ExchangeRateRepository exchangeRateRepository;

  private ExchangeRateReadServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ExchangeRateReadServiceImpl(
            new ExchangeRateQueryValidationHelper(),
            new ExchangeRateReadHelper(exchangeRateRepository),
            Mappers.getMapper(ExchangeRateReadMapper.class));
  }

  @Test
  void returnsLatestCrossRatesForRequestedPairsUsingOneBatchQuery() {
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, "1.10");
    ExchangeRateEntity eurToGbp = rate(GBP_CURRENCY, EXCHANGE_RATE_TEST_DATE, "0.85");
    when(exchangeRateRepository.findLatestEuroRates(new String[] {USD_CURRENCY, GBP_CURRENCY}))
        .thenReturn(List.of(eurToUsd, eurToGbp));

    var responses =
        service.retrieveExchangeRates(
            List.of(USD_CURRENCY, GBP_CURRENCY), List.of(GBP_CURRENCY, USD_CURRENCY));

    assertThat(responses).hasSize(2);
    assertThat(responses.getFirst().baseCurrency()).isEqualTo(USD_CURRENCY);
    assertThat(responses.getFirst().quoteCurrency()).isEqualTo(GBP_CURRENCY);
    assertThat(responses.getFirst().rate())
        .isEqualByComparingTo(
            new BigDecimal("0.85").divide(new BigDecimal("1.10"), MathContext.DECIMAL128));
    assertThat(responses.getFirst().effectiveDate())
        .isEqualTo(LocalDate.parse(EXCHANGE_RATE_TEST_DATE));
    assertThat(responses.get(1).rate())
        .isEqualByComparingTo(
            new BigDecimal("1.10").divide(new BigDecimal("0.85"), MathContext.DECIMAL128));
    verify(exchangeRateRepository).findLatestEuroRates(new String[] {USD_CURRENCY, GBP_CURRENCY});
  }

  @Test
  void omitsPairsWhenEitherRequiredPivotRateIsUnavailable() {
    when(exchangeRateRepository.findLatestEuroRates(new String[] {USD_CURRENCY, GBP_CURRENCY}))
        .thenReturn(List.of(rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, "1.10")));

    var responses = service.retrieveExchangeRates(List.of(USD_CURRENCY), List.of(GBP_CURRENCY));

    assertThat(responses).isEmpty();
  }

  @Test
  void returnsIdentityRateWithoutQueryingTheRepositoryForSameCurrency() {
    var responses = service.retrieveExchangeRates(List.of(USD_CURRENCY), List.of(USD_CURRENCY));

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().rate()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(responses.getFirst().effectiveDate()).isNull();
    verifyNoInteractions(exchangeRateRepository);
  }

  @Test
  void rejectsPivotRatesWithDifferentEffectiveDates() {
    when(exchangeRateRepository.findLatestEuroRates(new String[] {USD_CURRENCY, GBP_CURRENCY}))
        .thenReturn(
            List.of(
                rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, "1.10"),
                rate(GBP_CURRENCY, "2026-11-25", "0.85")));

    assertThatThrownBy(
            () -> service.retrieveExchangeRates(List.of(USD_CURRENCY), List.of(GBP_CURRENCY)))
        .isInstanceOf(ExchangeRateDataInconsistencyException.class)
        .hasMessageContaining("USD/GBP");
  }

  private ExchangeRateEntity rate(String quoteCurrency, String effectiveDate, String value) {
    return new ExchangeRateEntity(
        new ExchangeRateId(EUR_CURRENCY, quoteCurrency, LocalDate.parse(effectiveDate)),
        new BigDecimal(value));
  }
}
