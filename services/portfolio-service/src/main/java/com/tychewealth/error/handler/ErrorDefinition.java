package com.tychewealth.error.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDefinition {
  GENERIC_INTERNAL_ERROR("TYCHE-001", "GENERIC_INTERNAL_ERROR", "An unexpected error occurred"),
  GENERIC_VALIDATION_ERROR("TYCHE-002", "GENERIC_VALIDATION_ERROR", "Request validation failed"),
  GENERIC_BAD_REQUEST("TYCHE-003", "GENERIC_BAD_REQUEST", "The request is invalid: ${error:-}"),
  CONFLICT("TYCHE-005", "CONFLICT", "The operation conflicts with current state"),
  UNAUTHORIZED("TYCHE-006", "UNAUTHORIZED", "Authentication is required"),
  FORBIDDEN("TYCHE-007", "FORBIDDEN", "You do not have permission to perform this action"),
  RATE_LIMITED("TYCHE-008", "RATE_LIMITED", "Too many requests"),

  PORTFOLIO_NAME_CONFLICT(
      "TYCHE-400",
      "PORTFOLIO_NAME_CONFLICT",
      "A portfolio with name '${name:-}' already exists for this user");

  private final String code;
  private final String type;
  private final String description;
}
