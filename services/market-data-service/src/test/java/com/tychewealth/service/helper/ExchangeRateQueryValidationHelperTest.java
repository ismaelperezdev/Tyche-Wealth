package com.tychewealth.service.helper;

import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.TEST_EXCHANGE_RATE;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static com.tychewealth.testdata.ExchangeRateTestHelper.assertInvalidQuery;
import static com.tychewealth.testdata.ExchangeRateTestHelper.rate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.ExchangeRateDataInconsistencyException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExchangeRateQueryValidationHelperTest {

  private final ExchangeRateQueryValidationHelper helper = new ExchangeRateQueryValidationHelper();

  @Test
  void acceptsCurrencyCodesRegardlessOfCaseOrSurroundingWhitespace() {
    assertThatCode(() -> helper.validatePairs(List.of(" eur "), List.of("usd")))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsMismatchedCurrencyLists() {
    assertInvalidQuery(
        () -> helper.validatePairs(List.of("EUR"), List.of("USD", GBP_CURRENCY)),
        ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIR_COUNT_MISMATCH);
  }

  @Test
  void rejectsEmptyCurrencyLists() {
    assertInvalidQuery(
        () -> helper.validatePairs(List.of(), List.of()),
        ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIRS_REQUIRED);
  }

  @Test
  void rejectsCurrencyCodesThatAreNotThreeLetters() {
    assertInvalidQuery(
        () -> helper.validatePairs(List.of("US"), List.of("EUR")),
        ErrorDefinition.EXCHANGE_RATE_CURRENCY_INVALID);
  }

  @Test
  void reportsCurrencyPairAndDatesWhenPivotRateDatesDiffer() {
    ErrorDefinition errorDefinition = ErrorDefinition.EXCHANGE_RATE_EFFECTIVE_DATES_MISMATCH;
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE);
    ExchangeRateEntity eurToGbp = rate("GBP", "2026-11-25");

    assertThatThrownBy(
            () ->
                helper.validateEffectiveDates(
                    new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY), eurToUsd, eurToGbp))
        .isInstanceOfSatisfying(
            ExchangeRateDataInconsistencyException.class,
            exception -> {
              assertThat(exception.getErrorDefinition()).isEqualTo(errorDefinition);
              assertThat(exception.getMessage())
                  .isEqualTo(
                      errorDefinition
                          .getDescription()
                          .formatted(
                              USD_CURRENCY,
                              GBP_CURRENCY,
                              USD_CURRENCY,
                              EXCHANGE_RATE_TEST_DATE,
                              GBP_CURRENCY,
                              "2026-11-25"));
            });
  }

  @Test
  void rejectsNonPositiveBasePivotRates() {
    ErrorDefinition errorDefinition = ErrorDefinition.EXCHANGE_RATE_BASE_PIVOT_RATE_INVALID;
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, "0");
    ExchangeRateEntity eurToGbp = rate(GBP_CURRENCY, EXCHANGE_RATE_TEST_DATE, "0.85");

    assertThatThrownBy(
            () ->
                helper.validatePivotRates(
                    new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY), eurToUsd, eurToGbp))
        .isInstanceOfSatisfying(
            ExchangeRateDataInconsistencyException.class,
            exception -> {
              assertThat(exception.getErrorDefinition()).isEqualTo(errorDefinition);
              assertThat(exception.getMessage())
                  .isEqualTo(
                      errorDefinition
                          .getDescription()
                          .formatted(USD_CURRENCY, GBP_CURRENCY, USD_CURRENCY, 0));
            });
  }

  @Test
  void allowsZeroQuotePivotRate() {
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, TEST_EXCHANGE_RATE);
    ExchangeRateEntity eurToGbp = rate(GBP_CURRENCY, EXCHANGE_RATE_TEST_DATE, "0");

    assertThatCode(
            () ->
                helper.validatePivotRates(
                    new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY), eurToUsd, eurToGbp))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsNegativeQuotePivotRates() {
    ErrorDefinition errorDefinition = ErrorDefinition.EXCHANGE_RATE_QUOTE_PIVOT_RATE_INVALID;
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, TEST_EXCHANGE_RATE);
    ExchangeRateEntity eurToGbp = rate(GBP_CURRENCY, EXCHANGE_RATE_TEST_DATE, "-0.85");

    assertThatThrownBy(
            () ->
                helper.validatePivotRates(
                    new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY), eurToUsd, eurToGbp))
        .isInstanceOfSatisfying(
            ExchangeRateDataInconsistencyException.class,
            exception -> {
              assertThat(exception.getErrorDefinition()).isEqualTo(errorDefinition);
              assertThat(exception.getMessage())
                  .isEqualTo(
                      errorDefinition
                          .getDescription()
                          .formatted(USD_CURRENCY, GBP_CURRENCY, GBP_CURRENCY, "-0.85"));
            });
  }
}
