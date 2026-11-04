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
      "bucketStart filters must use ISO-8601 instants");

  private final String code;
  private final String type;
  private final String description;
}
