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
  RATE_LIMIT_BACKEND_UNAVAILABLE(
      "TYCHE-009", "RATE_LIMIT_BACKEND_UNAVAILABLE", "rate-limit-backend/unavailable"),

  PORTFOLIO_NAME_CONFLICT(
      "TYCHE-400",
      "PORTFOLIO_NAME_CONFLICT",
      "A portfolio with name '${name:-}' already exists for this user"),
  PORTFOLIO_LIMIT_REACHED(
      "TYCHE-401",
      "PORTFOLIO_LIMIT_REACHED",
      "The maximum number of portfolios allowed per user has been reached"),
  PORTFOLIO_NOT_FOUND("TYCHE-402", "PORTFOLIO_NOT_FOUND", "The portfolio was not found"),

  ASSET_IMPORT_EXTRACTION_FAILED(
      "TYCHE-500",
      "ASSET_IMPORT_EXTRACTION_FAILED",
      "Unable to extract text from file. Expected: ${expected:-}. Received: ${received:-}"),
  ATTACHMENT_SIZE_LIMIT_EXCEEDED(
      "TYCHE-501",
      "ATTACHMENT_SIZE_LIMIT_EXCEEDED",
      "The attachment exceeds the maximum allowed size. Maximum: ${expected:-} bytes. Received: ${received:-} bytes"),
  ATTACHMENT_PAGE_LIMIT_EXCEEDED(
      "TYCHE-502",
      "ATTACHMENT_PAGE_LIMIT_EXCEEDED",
      "The attachment exceeds the maximum allowed page count. Maximum: ${expected:-}. Received: ${received:-}"),
  ATTACHMENT_TEXT_LIMIT_EXCEEDED(
      "TYCHE-503",
      "ATTACHMENT_TEXT_LIMIT_EXCEEDED",
      "The extracted attachment text exceeds the maximum allowed length. Maximum: ${expected:-} characters. Received: ${received:-} characters"),
  ATTACHMENT_INSPECTION_FAILED(
      "TYCHE-504", "ATTACHMENT_INSPECTION_FAILED", "Unable to inspect attachment: ${error:-}"),
  ASSET_IMPORT_AI_RESPONSE_INVALID(
      "TYCHE-505",
      "ASSET_IMPORT_AI_RESPONSE_INVALID",
      "The AI response could not be converted into valid asset data: ${error:-}"),
  ATTACHMENT_PROCESSING_TIMEOUT_EXCEEDED(
      "TYCHE-506",
      "ATTACHMENT_PROCESSING_TIMEOUT_EXCEEDED",
      "Attachment processing exceeded the maximum allowed time. Maximum: ${expected:-} seconds. Received: ${received:-} seconds"),
  AI_PROCESSING_TIMEOUT_EXCEEDED(
      "TYCHE-507",
      "AI_PROCESSING_TIMEOUT_EXCEEDED",
      "AI processing exceeded the maximum allowed time. Maximum: ${expected:-} seconds. Received: ${received:-} seconds"),
  ASSET_IMPORT_RESULT_LIMIT_EXCEEDED(
      "TYCHE-508",
      "ASSET_IMPORT_RESULT_LIMIT_EXCEEDED",
      "The import produced too many assets. Maximum: ${expected:-}. Received: ${received:-}");

  private final String code;
  private final String type;
  private final String description;
}
