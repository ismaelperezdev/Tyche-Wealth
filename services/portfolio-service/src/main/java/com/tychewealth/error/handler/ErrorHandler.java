package com.tychewealth.error.handler;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.ERROR_PLACEHOLDER;
import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.CommonConstants.NAME_PLACEHOLDER;
import static com.tychewealth.constants.LogConstants.ERROR_HANDLER_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT_WITH_URI;
import static com.tychewealth.constants.LogConstants.SYSTEM;
import static com.tychewealth.constants.LogConstants.UNHANDLED_EXCEPTION_MESSAGE;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.exception.PortfolioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(PortfolioException.class)
  public ResponseEntity<ErrorResponse> handlePortfolioException(PortfolioException ex) {
    return build(ex.getErrorDefinition(), ex.getHttpStatus(), ex.getMessage(), ex.getMetadata());
  }

  @ExceptionHandler(AssetImportException.class)
  public ResponseEntity<ErrorResponse> handleAssetImportException(AssetImportException ex) {
    return build(ex.getErrorDefinition(), ex.getHttpStatus(), ex.getMessage(), ex.getMetadata());
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
    return build(ex.getErrorDefinition(), ex.getHttpStatus(), ex.getMessage(), ex.getMetadata());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    String details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldMessage)
            .collect(Collectors.joining("; "));
    return build(ErrorDefinition.GENERIC_VALIDATION_ERROR, HttpStatus.BAD_REQUEST, details, null);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    return build(
        ErrorDefinition.GENERIC_VALIDATION_ERROR, HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    Map<String, String> metadata = Map.of(ERROR, resolveErrorMessage(ex));
    return build(
        ErrorDefinition.GENERIC_BAD_REQUEST,
        HttpStatus.BAD_REQUEST,
        ErrorDefinition.GENERIC_BAD_REQUEST.getDescription(),
        metadata);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    return build(
        ErrorDefinition.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorDefinition.CONFLICT.getDescription(),
        null);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    ErrorDefinition definition = mapByStatus(status);
    String description = ex.getReason() == null ? definition.getDescription() : ex.getReason();
    return build(definition, status, description, null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    log.error(
        REQUEST_CONFLICT_WITH_URI,
        SYSTEM,
        ERROR_HANDLER_ACTION,
        UNHANDLED_EXCEPTION_MESSAGE,
        request == null ? "unknown" : request.getRequestURI(),
        ex);
    return build(
        ErrorDefinition.GENERIC_INTERNAL_ERROR,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorDefinition.GENERIC_INTERNAL_ERROR.getDescription(),
        null);
  }

  private ResponseEntity<ErrorResponse> build(
      ErrorDefinition definition,
      HttpStatus status,
      String description,
      Map<String, String> metadata) {
    ErrorResponse response =
        ErrorResponse.builder()
            .code(definition.getCode())
            .type(definition.getType())
            .description(resolveDescription(definition, description, metadata))
            .build();

    return ResponseEntity.status(status).body(response);
  }

  private String resolveDescription(
      ErrorDefinition definition, String description, Map<String, String> metadata) {
    String resolvedDescription =
        description == null || description.isBlank() ? definition.getDescription() : description;

    if (metadata == null || metadata.isEmpty()) {
      return resolvedDescription.replace(NAME_PLACEHOLDER, "").replace(ERROR_PLACEHOLDER, "");
    }

    String name = metadata.getOrDefault(NAME, "");
    String error = metadata.getOrDefault(ERROR, "");
    return resolvedDescription.replace(NAME_PLACEHOLDER, name).replace(ERROR_PLACEHOLDER, error);
  }

  private String resolveErrorMessage(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getMostSpecificCause();
    if (cause instanceof InvalidFormatException invalidFormatException) {
      return resolveInvalidFormatMessage(invalidFormatException);
    }
    if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
      return cause.getMessage();
    }
    if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
      return ex.getMessage();
    }
    return "unknown bad request";
  }

  private String resolveInvalidFormatMessage(InvalidFormatException ex) {
    if (ex.getTargetType() != null && ex.getTargetType().isEnum()) {
      String fieldName = ex.getPath().isEmpty() ? "unknown" : ex.getPath().getLast().getFieldName();
      String invalidValue = ex.getValue() == null ? "null" : ex.getValue().toString();
      String availableOptions =
          Arrays.stream(ex.getTargetType().getEnumConstants())
              .map(String::valueOf)
              .collect(Collectors.joining(", "));
      return String.format(
          "The option '%s' is not available for field '%s'. Available options: %s",
          invalidValue, fieldName, availableOptions);
    }

    if (ex.getOriginalMessage() != null && !ex.getOriginalMessage().isBlank()) {
      return ex.getOriginalMessage();
    }
    return "invalid request payload";
  }

  private String toFieldMessage(FieldError error) {
    String message =
        error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
    return error.getField() + ": " + message;
  }

  private ErrorDefinition mapByStatus(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> ErrorDefinition.GENERIC_BAD_REQUEST;
      case UNAUTHORIZED -> ErrorDefinition.UNAUTHORIZED;
      case FORBIDDEN -> ErrorDefinition.FORBIDDEN;
      case CONFLICT -> ErrorDefinition.CONFLICT;
      case TOO_MANY_REQUESTS -> ErrorDefinition.RATE_LIMITED;
      default -> ErrorDefinition.GENERIC_INTERNAL_ERROR;
    };
  }
}
