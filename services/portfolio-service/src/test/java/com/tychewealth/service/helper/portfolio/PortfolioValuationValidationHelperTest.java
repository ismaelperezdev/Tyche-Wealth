package com.tychewealth.service.helper.portfolio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PortfolioValuationValidationHelperTest {

  private final PortfolioValuationValidationHelper validationHelper =
      new PortfolioValuationValidationHelper();

  @Test
  void acceptsOptionalOrOrderedRange() {
    assertDoesNotThrow(() -> validationHelper.validateRange(null, null));
    assertDoesNotThrow(
        () ->
            validationHelper.validateRange(
                Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-02T00:00:00Z")));
  }

  @Test
  void rejectsReversedRange() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                validationHelper.validateRange(
                    Instant.parse("2026-11-02T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")));

    assertEquals(
        ErrorDefinition.PORTFOLIO_MARKET_DATA_TIME_RANGE_INVALID, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }
}
