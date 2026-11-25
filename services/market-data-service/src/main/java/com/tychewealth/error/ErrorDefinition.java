package com.tychewealth.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDefinition {
  GENERIC_INTERNAL_ERROR("TYCHE-600", "GENERIC_INTERNAL_ERROR", "An unexpected error occurred"),
  GENERIC_BAD_REQUEST("TYCHE-601", "GENERIC_BAD_REQUEST", "The request is invalid"),
  GENERIC_UNAUTHORIZED("TYCHE-602", "GENERIC_UNAUTHORIZED", "Authentication is required"),
  GENERIC_FORBIDDEN(
      "TYCHE-603", "GENERIC_FORBIDDEN", "You do not have permission to perform this action"),
  GENERIC_NOT_FOUND("TYCHE-604", "GENERIC_NOT_FOUND", "The requested resource was not found"),
  GENERIC_CONFLICT("TYCHE-605", "GENERIC_CONFLICT", "The request conflicts with the current state"),
  INVALID_MARKET_PRICE_QUERY(
      "TYCHE-606", "INVALID_MARKET_PRICE_QUERY", "The market-price query is invalid"),
  MARKET_PRICE_SYMBOL_FILTER_REQUIRED(
      "TYCHE-607", "MARKET_PRICE_SYMBOL_FILTER_REQUIRED", "A symbol filter is required"),
  MARKET_PRICE_SYMBOLS_REQUIRED(
      "TYCHE-608", "MARKET_PRICE_SYMBOLS_REQUIRED", "At least one symbol is required"),
  MARKET_PRICE_SYMBOL_BLANK("TYCHE-609", "MARKET_PRICE_SYMBOL_BLANK", "Symbols must not be blank"),
  MARKET_PRICE_SYMBOL_TOO_LONG(
      "TYCHE-610", "MARKET_PRICE_SYMBOL_TOO_LONG", "A symbol exceeds the maximum length"),
  MARKET_PRICE_TIME_RANGE_INVALID(
      "TYCHE-611", "MARKET_PRICE_TIME_RANGE_INVALID", "The market-price time range is invalid"),
  MARKET_PRICE_CAPTURED_AT_INVALID(
      "TYCHE-612",
      "MARKET_PRICE_CAPTURED_AT_INVALID",
      "capturedAt filters must use ISO-8601 instants"),
  MARKET_PRICE_BUCKET_START_INVALID(
      "TYCHE-613",
      "MARKET_PRICE_BUCKET_START_INVALID",
      "bucketStart filters must use ISO-8601 instants"),
  EXCHANGE_RATE_CURRENCY_PAIR_COUNT_MISMATCH(
      "TYCHE-614",
      "EXCHANGE_RATE_CURRENCY_PAIR_COUNT_MISMATCH",
      "The number of base currencies must match the number of quote currencies"),
  EXCHANGE_RATE_CURRENCY_PAIRS_REQUIRED(
      "TYCHE-615",
      "EXCHANGE_RATE_CURRENCY_PAIRS_REQUIRED",
      "At least one currency pair is required"),
  EXCHANGE_RATE_CURRENCY_INVALID(
      "TYCHE-616", "EXCHANGE_RATE_CURRENCY_INVALID", "Currencies must be three-letter codes"),
  EXCHANGE_RATE_EFFECTIVE_DATES_MISMATCH(
      "TYCHE-617",
      "EXCHANGE_RATE_EFFECTIVE_DATES_MISMATCH",
      "Cannot calculate %s/%s: EUR/%s is effective on %s but EUR/%s is effective on %s"),
  EXCHANGE_RATE_BASE_PIVOT_RATE_INVALID(
      "TYCHE-618",
      "EXCHANGE_RATE_BASE_PIVOT_RATE_INVALID",
      "Cannot calculate %s/%s: EUR/%s base pivot rate must be positive, but was %s"),
  EXCHANGE_RATE_QUOTE_PIVOT_RATE_INVALID(
      "TYCHE-619",
      "EXCHANGE_RATE_QUOTE_PIVOT_RATE_INVALID",
      "Cannot calculate %s/%s: EUR/%s quote pivot rate must not be negative, but was %s"),

  // MARKET AUTHENTICATION
  MARKET_AUTHENTICATION_SERVICE_UNAVAILABLE(
      "TYCHE-620",
      "MARKET_AUTHENTICATION_SERVICE_UNAVAILABLE",
      "The authentication service is temporarily unavailable");

  private final String code;
  private final String type;
  private final String description;
}
