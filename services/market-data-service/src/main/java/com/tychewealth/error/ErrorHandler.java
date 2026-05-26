package com.tychewealth.error;

import static com.tychewealth.constants.LogConstants.ERROR_HANDLER;
import static com.tychewealth.constants.LogConstants.HANDLE_ACTION;
import static com.tychewealth.constants.LogConstants.UNHANDLED_EXCEPTION;
import static com.tychewealth.error.ErrorDefinition.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    ErrorDefinition errorDefinition = mapByStatus(status);
    String reason = ex.getReason();

    String description =
        reason == null || reason.isBlank() ? errorDefinition.getDescription() : reason;

    return build(status, errorDefinition, description);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error(UNHANDLED_EXCEPTION, ERROR_HANDLER, HANDLE_ACTION, ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        GENERIC_INTERNAL_ERROR,
        GENERIC_INTERNAL_ERROR.getDescription());
  }

  private ErrorDefinition mapByStatus(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> GENERIC_BAD_REQUEST;
      case UNAUTHORIZED -> GENERIC_UNAUTHORIZED;
      case FORBIDDEN -> GENERIC_FORBIDDEN;
      case NOT_FOUND -> GENERIC_NOT_FOUND;
      case CONFLICT -> GENERIC_CONFLICT;
      default -> GENERIC_INTERNAL_ERROR;
    };
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, ErrorDefinition errorDefinition, String description) {
    ErrorResponse response =
        ErrorResponse.builder()
            .code(errorDefinition.getCode())
            .type(errorDefinition.getType())
            .description(description)
            .build();

    return ResponseEntity.status(status).body(response);
  }
}
