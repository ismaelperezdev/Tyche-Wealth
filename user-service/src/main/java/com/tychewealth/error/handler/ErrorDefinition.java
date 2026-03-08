package com.tychewealth.error.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDefinition {
  GENERIC_INTERNAL_ERROR("TYCHE-001", "GENERIC_INTERNAL_ERROR", "An unexpected error occurred"),
  GENERIC_VALIDATION_ERROR("TYCHE-002", "GENERIC_VALIDATION_ERROR", "Request validation failed"),
  GENERIC_BAD_REQUEST("TYCHE-003", "GENERIC_BAD_REQUEST", "The request is invalid"),
  RESOURCE_NOT_FOUND("TYCHE-004", "RESOURCE_NOT_FOUND", "Requested resource was not found"),
  CONFLICT("TYCHE-005", "CONFLICT", "The operation conflicts with current state"),
  UNAUTHORIZED("TYCHE-006", "UNAUTHORIZED", "Authentication is required"),
  FORBIDDEN("TYCHE-007", "FORBIDDEN", "You do not have permission to perform this action"),

  // AUTH
  AUTH_REGISTRATION_CONFLICT(
      "TYCHE-100",
      "AUTH_REGISTRATION_CONFLICT",
      "A user with the provided credentials already exists");

  private final String code;
  private final String type;
  private final String description;
}
