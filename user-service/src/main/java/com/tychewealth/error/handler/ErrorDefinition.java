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
  AUTH_EMAIL_ALREADY_EXISTS_ERROR(
      "TYCHE-100",
      "AUTH_EMAIL_ALREADY_EXISTS_ERROR",
      "The email ${email:-} is already linked to other user"),
  AUTH_USERNAME_ALREADY_EXISTS_ERROR(
      "TYCHE-101",
      "AUTH_USERNAME_ALREADY_EXISTS_ERROR",
      "The username ${username:-} is already linked to other user");

  private final String code;
  private final String type;
  private final String description;
}
