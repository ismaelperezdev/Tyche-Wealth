package com.tychewealth.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ErrorHandlerTest {

  private static final String VEHICLE_ALREADY_EXISTS_DESCRIPTION = "Vehicle already exists";

  private final ErrorHandler errorHandler = new ErrorHandler();

  @Test
  void shouldReturnInternalServerErrorForGenericException() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleGenericException(new RuntimeException("Unexpected"));

    assertErrorResponse(
        response,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorDefinition.GENERIC_INTERNAL_ERROR,
        ErrorDefinition.GENERIC_INTERNAL_ERROR.getDescription());
  }

  @Test
  void shouldReturnBadRequestErrorWhenResponseStatusExceptionHasBadRequestStatus() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleResponseStatusException(
            new ResponseStatusException(HttpStatus.BAD_REQUEST));

    assertErrorResponse(
        response,
        HttpStatus.BAD_REQUEST,
        ErrorDefinition.GENERIC_BAD_REQUEST,
        ErrorDefinition.GENERIC_BAD_REQUEST.getDescription());
  }

  @Test
  void shouldReturnNotFoundErrorWhenResponseStatusExceptionHasNotFoundStatus() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleResponseStatusException(
            new ResponseStatusException(HttpStatus.NOT_FOUND));

    assertErrorResponse(
        response,
        HttpStatus.NOT_FOUND,
        ErrorDefinition.GENERIC_NOT_FOUND,
        ErrorDefinition.GENERIC_NOT_FOUND.getDescription());
  }

  @Test
  void shouldUseReasonAsDescriptionWhenResponseStatusExceptionProvidesIt() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleResponseStatusException(
            new ResponseStatusException(HttpStatus.CONFLICT, VEHICLE_ALREADY_EXISTS_DESCRIPTION));

    assertErrorResponse(
        response,
        HttpStatus.CONFLICT,
        ErrorDefinition.GENERIC_CONFLICT,
        VEHICLE_ALREADY_EXISTS_DESCRIPTION);
  }

  @Test
  void shouldReturnInternalErrorDefinitionForUnhandledResponseStatusExceptionStatus() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleResponseStatusException(
            new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));

    assertErrorResponse(
        response,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorDefinition.GENERIC_INTERNAL_ERROR,
        ErrorDefinition.GENERIC_INTERNAL_ERROR.getDescription());
  }

  private void assertErrorResponse(
      ResponseEntity<ErrorResponse> response,
      HttpStatus expectedStatus,
      ErrorDefinition expectedErrorDefinition,
      String expectedDescription) {
    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(expectedErrorDefinition.getCode());
    assertThat(response.getBody().getType()).isEqualTo(expectedErrorDefinition.getType());
    assertThat(response.getBody().getDescription()).isEqualTo(expectedDescription);
  }
}
