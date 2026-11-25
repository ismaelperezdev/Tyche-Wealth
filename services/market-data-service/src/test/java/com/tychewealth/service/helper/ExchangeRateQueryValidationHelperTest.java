package com.tychewealth.service.helper;

import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.entity.ExchangeRateId;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.ExchangeRateDataInconsistencyException;
import com.tychewealth.error.exception.InvalidExchangeRateQueryException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
    ExchangeRateEntity eurToUsd = rate("USD", EXCHANGE_RATE_TEST_DATE);
    ExchangeRateEntity eurToGbp = rate("GBP", "2026-11-25");

    assertThatThrownBy(
            () ->
                helper.validateEffectiveDates(
                    new CurrencyPairDto("USD", "GBP"), eurToUsd, eurToGbp))
        .isInstanceOf(ExchangeRateDataInconsistencyException.class)
        .hasMessageContaining("USD/GBP")
        .hasMessageContaining(EXCHANGE_RATE_TEST_DATE)
        .hasMessageContaining("2026-11-25");
  }

  private ExchangeRateEntity rate(String quoteCurrency, String effectiveDate) {
    return new ExchangeRateEntity(
        new ExchangeRateId("EUR", quoteCurrency, java.time.LocalDate.parse(effectiveDate)),
        java.math.BigDecimal.ONE);
  }

  private void assertInvalidQuery(ThrowingCallable request, ErrorDefinition expectedError) {
    assertThatThrownBy(request)
        .isInstanceOfSatisfying(
            InvalidExchangeRateQueryException.class,
            exception -> assertThat(exception.getErrorDefinition()).isEqualTo(expectedError));
  }
}
