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
  GENERIC_CONFLICT("ERROR-006", "GENERIC_CONFLICT", "The request conflicts with the current state");

  private final String code;
  private final String type;
  private final String description;
}
