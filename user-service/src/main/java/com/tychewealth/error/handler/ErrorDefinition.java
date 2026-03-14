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
  RATE_LIMITED("TYCHE-008", "RATE_LIMITED", "Too many requests"),

  // AUTH
  AUTH_REGISTRATION_CONFLICT(
      "TYCHE-100",
      "AUTH_REGISTRATION_CONFLICT",
      "A user with the provided credentials already exists"),
  AUTH_LOGIN_INVALID_CREDENTIALS(
      "TYCHE-101", "AUTH_LOGIN_INVALID_CREDENTIALS", "The provided login credentials are invalid"),
  AUTH_LOGIN_PASSWORD_FORMAT_INVALID(
      "TYCHE-102",
      "AUTH_LOGIN_PASSWORD_FORMAT_INVALID",
      "Password must be 8-72 characters and include at least one uppercase letter, one lowercase letter, one number, and one symbol"),
  AUTH_REFRESH_TOKEN_INVALID(
      "TYCHE-103", "AUTH_REFRESH_TOKEN_INVALID", "The provided refresh token is invalid"),
  AUTH_REGISTER_PASSWORD_FORMAT_INVALID(
      "TYCHE-104",
      "AUTH_REGISTER_PASSWORD_FORMAT_INVALID",
      "Password must be 8-72 characters and include at least one uppercase letter, one lowercase letter, one number, and one symbol"),

  // USER
  USER_NOT_FOUND("TYCHE-200", "USER_NOT_FOUND", "The requested user was not found"),
  USER_USERNAME_CONFLICT(
      "TYCHE-201", "USER_USERNAME_CONFLICT", "A user with the provided username already exists"),
  USER_CURRENT_PASSWORD_INVALID(
      "TYCHE-202", "USER_CURRENT_PASSWORD_INVALID", "The provided current password is invalid"),
  USER_NEW_PASSWORD_MUST_BE_DIFFERENT(
      "TYCHE-203",
      "USER_NEW_PASSWORD_MUST_BE_DIFFERENT",
      "The new password must be different from the current password");

  private final String code;
  private final String type;
  private final String description;
}
