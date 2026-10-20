package com.tychewealth.listing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stable API error identifiers produced by listing parsing and pagination validation.
 *
 * <p>Codes and types are part of the response contract. Descriptions may contain numbered {@link
 * java.text.MessageFormat} placeholders filled by {@link
 * com.tychewealth.listing.exception.FilterException}.
 */
@Getter
@AllArgsConstructor
public enum ErrorDefinition {
  PAGINATION_INVALID_OFFSET(
      "TYCHE-9100",
      "PAGINATION_INVALID_OFFSET",
      "Pagination offset must be greater than or equal to zero"),

  PAGINATION_INVALID_LIMIT(
      "TYCHE-9101",
      "PAGINATION_INVALID_LIMIT",
      "Pagination limit must be greater than or equal to one"),

  PAGINATION_INVALID_TOTAL(
      "TYCHE-9108",
      "PAGINATION_INVALID_TOTAL",
      "Pagination total must be greater than or equal to zero"),

  RANGE_PARSE_ERROR(
      "TYCHE-9102", "RANGE_PARSE_ERROR", "The range filter value could not be parsed: {0}"),

  RANGE_INVALID_VALUES("TYCHE-9103", "RANGE_INVALID_VALUES", "The range values are invalid"),

  FILTER_MAX_EXCEEDED(
      "TYCHE-9104",
      "FILTER_MAX_EXCEEDED",
      "The maximum number of sorts ({0}) has been exceeded: {1} sorts provided"),

  FILTER_PARSE_ERROR(
      "TYCHE-9105", "FILTER_PARSE_ERROR", "The filter expression could not be parsed"),

  FILTER_FIELD_NOT_FOUND(
      "TYCHE-9106", "FILTER_FIELD_NOT_FOUND", "The requested filter field was not found: {0}"),

  FILTER_OPERATOR_NOT_ALLOWED(
      "TYCHE-9107", "FILTER_OPERATOR_NOT_ALLOWED", "The filter operator is not allowed: {0}");

  private final String code;
  private final String type;
  private final String description;
}
