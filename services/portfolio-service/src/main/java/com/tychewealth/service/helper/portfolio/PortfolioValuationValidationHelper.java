package com.tychewealth.service.helper.portfolio;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_TIME_RANGE_INVALID;

import com.tychewealth.error.exception.PortfolioException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Validates requests for historical portfolio valuation. */
@Component
public class PortfolioValuationValidationHelper {

  public void validateRange(Instant from, Instant to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new PortfolioException(
          PORTFOLIO_MARKET_DATA_TIME_RANGE_INVALID,
          Map.of(ERROR, "from must not be after to"),
          HttpStatus.BAD_REQUEST);
    }
  }
}
