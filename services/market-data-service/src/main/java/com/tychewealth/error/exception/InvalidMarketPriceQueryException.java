package com.tychewealth.error.exception;

import com.tychewealth.error.ErrorDefinition;
import lombok.Getter;

/** Raised when a market-price read request contains invalid query parameters. */
@Getter
public class InvalidMarketPriceQueryException extends RuntimeException {

  private final ErrorDefinition errorDefinition;

  public InvalidMarketPriceQueryException(String message) {
    this(ErrorDefinition.INVALID_MARKET_PRICE_QUERY, message);
  }

  public InvalidMarketPriceQueryException(ErrorDefinition errorDefinition) {
    this(errorDefinition, errorDefinition.getDescription());
  }

  private InvalidMarketPriceQueryException(ErrorDefinition errorDefinition, String message) {
    super(message);
    this.errorDefinition = errorDefinition;
  }
}
