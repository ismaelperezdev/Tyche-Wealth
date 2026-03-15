package com.tychewealth.error.handler;

import static com.tychewealth.constants.ApiConstants.USER_BASE_URL;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.service.monitoring.UserMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class ErrorHandler {

  private final UserMetrics userMetrics;

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(
      AuthException ex, HttpServletRequest request) {
    if (isUserRequest(request)) {
      userMetrics.recordUnauthorized();
    }
    return buildFromException(ex.getErrorDefinition(), ex.getHttpStatus());
  }

  @ExceptionHandler(UserException.class)
  public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
    return buildFromException(ex.getErrorDefinition(), ex.getHttpStatus());
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

  private ResponseEntity<ErrorResponse> buildFromException(
      ErrorDefinition errorDefinition, HttpStatus httpStatus) {
    ErrorDefinition definition =
        errorDefinition != null ? errorDefinition : ErrorDefinition.CONFLICT;
    HttpStatus status = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
    return build(definition, status, definition.getDescription());
  }

  private ErrorDefinition mapByStatus(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> ErrorDefinition.GENERIC_BAD_REQUEST;
      case UNAUTHORIZED -> ErrorDefinition.UNAUTHORIZED;
      case FORBIDDEN -> ErrorDefinition.FORBIDDEN;
      case NOT_FOUND -> ErrorDefinition.RESOURCE_NOT_FOUND;
      case CONFLICT -> ErrorDefinition.CONFLICT;
      case TOO_MANY_REQUESTS -> ErrorDefinition.RATE_LIMITED;
      default -> ErrorDefinition.GENERIC_INTERNAL_ERROR;
    };
  }

  private String toFieldMessage(FieldError error) {
    String message =
        error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
    if ("AssertTrue".equals(error.getCode())) {
      return message;
    }
    return error.getField() + ": " + message;
  }

  private boolean isUserRequest(HttpServletRequest request) {
    return request != null
        && request.getRequestURI() != null
        && request.getRequestURI().startsWith(USER_BASE_URL);
  }
}
