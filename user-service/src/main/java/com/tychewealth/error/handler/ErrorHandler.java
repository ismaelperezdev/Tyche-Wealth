package com.tychewealth.error.handler;

import com.tychewealth.error.exception.AuthException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
    ErrorDefinition definition =
        ex.getErrorDefinition() != null ? ex.getErrorDefinition() : ErrorDefinition.CONFLICT;
    HttpStatus status = ex.getHttpStatus() == null ? HttpStatus.CONFLICT : ex.getHttpStatus();
    return build(definition, status, definition.getDescription());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    String details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldMessage)
            .collect(Collectors.joining("; "));

    return build(ErrorDefinition.GENERIC_VALIDATION_ERROR, HttpStatus.BAD_REQUEST, details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    return build(ErrorDefinition.GENERIC_VALIDATION_ERROR, HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    return build(
        ErrorDefinition.GENERIC_BAD_REQUEST,
        HttpStatus.BAD_REQUEST,
        ErrorDefinition.GENERIC_BAD_REQUEST.getDescription());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    ErrorDefinition definition = mapByStatus(status);
    String description = ex.getReason() == null ? definition.getDescription() : ex.getReason();
    return build(definition, status, description);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {

    return build(
        ErrorDefinition.CONFLICT, HttpStatus.CONFLICT, ErrorDefinition.CONFLICT.getDescription());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    return build(
        ErrorDefinition.GENERIC_INTERNAL_ERROR,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorDefinition.GENERIC_INTERNAL_ERROR.getDescription());
  }

  private ResponseEntity<ErrorResponse> build(
      ErrorDefinition definition, HttpStatus status, String description) {
    ErrorResponse response =
        ErrorResponse.builder()
            .code(definition.getCode())
            .type(definition.getType())
            .description(
                description == null || description.isBlank()
                    ? definition.getDescription()
                    : description)
            .build();

    return ResponseEntity.status(status).body(response);
  }

  private ErrorDefinition mapByStatus(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> ErrorDefinition.GENERIC_BAD_REQUEST;
      case UNAUTHORIZED -> ErrorDefinition.UNAUTHORIZED;
      case FORBIDDEN -> ErrorDefinition.FORBIDDEN;
      case NOT_FOUND -> ErrorDefinition.RESOURCE_NOT_FOUND;
      case CONFLICT -> ErrorDefinition.CONFLICT;
      default -> ErrorDefinition.GENERIC_INTERNAL_ERROR;
    };
  }

  private String toFieldMessage(FieldError error) {
    String message =
        error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
    return error.getField() + ": " + message;
  }
}
