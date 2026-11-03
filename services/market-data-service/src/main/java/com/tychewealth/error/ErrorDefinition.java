package com.tychewealth.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDefinition {
  GENERIC_INTERNAL_ERROR("ERROR-001", "GENERIC_INTERNAL_ERROR", "An unexpected error occurred"),
  GENERIC_BAD_REQUEST("ERROR-002", "GENERIC_BAD_REQUEST", "The request is invalid"),
  GENERIC_UNAUTHORIZED("ERROR-003", "GENERIC_UNAUTHORIZED", "Authentication is required"),
  GENERIC_FORBIDDEN(
      "ERROR-004", "GENERIC_FORBIDDEN", "You do not have permission to perform this action"),
  GENERIC_NOT_FOUND("ERROR-005", "GENERIC_NOT_FOUND", "The requested resource was not found"),
  GENERIC_CONFLICT("ERROR-006", "GENERIC_CONFLICT", "The request conflicts with the current state"),
  INVALID_MARKET_PRICE_QUERY(
      "ERROR-007", "INVALID_MARKET_PRICE_QUERY", "The market-price query is invalid"),
  MARKET_PRICE_SYMBOL_FILTER_REQUIRED(
      "ERROR-008", "MARKET_PRICE_SYMBOL_FILTER_REQUIRED", "A symbol filter is required"),
  MARKET_PRICE_SYMBOLS_REQUIRED(
      "ERROR-009", "MARKET_PRICE_SYMBOLS_REQUIRED", "At least one symbol is required"),
  MARKET_PRICE_SYMBOL_BLANK("ERROR-010", "MARKET_PRICE_SYMBOL_BLANK", "Symbols must not be blank"),
  MARKET_PRICE_SYMBOL_TOO_LONG(
      "ERROR-011", "MARKET_PRICE_SYMBOL_TOO_LONG", "A symbol exceeds the maximum length"),
  MARKET_PRICE_TIME_RANGE_INVALID(
      "ERROR-012", "MARKET_PRICE_TIME_RANGE_INVALID", "The market-price time range is invalid"),
  MARKET_PRICE_CAPTURED_AT_INVALID(
      "ERROR-013",
      "MARKET_PRICE_CAPTURED_AT_INVALID",
      "capturedAt filters must use ISO-8601 instants");

  private final String code;
  private final String type;
  private final String description;
}
