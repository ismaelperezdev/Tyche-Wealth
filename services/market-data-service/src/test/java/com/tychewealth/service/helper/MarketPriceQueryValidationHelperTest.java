package com.tychewealth.service.helper;

import static com.tychewealth.constants.TestConstants.BLANK_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.OVERSIZED_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.PADDED_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketPriceQueryValidationHelperTest {

  private final MarketPriceQueryValidationHelper helper = new MarketPriceQueryValidationHelper();

  @Test
  void normalizesAndDeduplicatesSymbols() {
    assertThat(
            helper.normalizeSymbols(List.of(PADDED_TEST_SYMBOL, TEST_SYMBOL, SECOND_TEST_SYMBOL)))
        .containsExactly(TEST_SYMBOL, SECOND_TEST_SYMBOL);
  }

  @Test
  void rejectsMissingSymbols() {
    assertThatThrownBy(() -> helper.normalizeSymbols(List.of()))
        .isInstanceOf(InvalidMarketPriceQueryException.class)
        .extracting(
            exception -> ((InvalidMarketPriceQueryException) exception).getErrorDefinition())
        .isEqualTo(ErrorDefinition.MARKET_PRICE_SYMBOLS_REQUIRED);
  }

  @Test
  void rejectsBlankSymbols() {
    assertThatThrownBy(() -> helper.normalizeSymbols(List.of(TEST_SYMBOL, BLANK_TEST_SYMBOL)))
        .isInstanceOf(InvalidMarketPriceQueryException.class)
        .extracting(
            exception -> ((InvalidMarketPriceQueryException) exception).getErrorDefinition())
        .isEqualTo(ErrorDefinition.MARKET_PRICE_SYMBOL_BLANK);
  }

  @Test
  void rejectsSymbolsLongerThanCatalogAllows() {
    assertThatThrownBy(() -> helper.normalizeSymbols(List.of(OVERSIZED_TEST_SYMBOL)))
        .isInstanceOf(InvalidMarketPriceQueryException.class)
        .extracting(
            exception -> ((InvalidMarketPriceQueryException) exception).getErrorDefinition())
        .isEqualTo(ErrorDefinition.MARKET_PRICE_SYMBOL_TOO_LONG);
  }

  @Test
  void rejectsInvalidTimeRanges() {
    assertThatThrownBy(
            () ->
                helper.validateTimeRange(
                    java.time.Instant.parse("2026-01-02T00:00:00Z"),
                    java.time.Instant.parse("2026-01-01T00:00:00Z")))
        .isInstanceOf(InvalidMarketPriceQueryException.class)
        .extracting(
            exception -> ((InvalidMarketPriceQueryException) exception).getErrorDefinition())
        .isEqualTo(ErrorDefinition.MARKET_PRICE_TIME_RANGE_INVALID);
  }
}
