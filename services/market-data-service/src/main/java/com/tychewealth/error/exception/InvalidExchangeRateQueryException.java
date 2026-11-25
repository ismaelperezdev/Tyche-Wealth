package com.tychewealth.error.exception;

import com.tychewealth.error.ErrorDefinition;
import lombok.Getter;

/** Raised when an exchange-rate read request contains invalid query parameters. */
@Getter
public class InvalidExchangeRateQueryException extends RuntimeException {

  private final ErrorDefinition errorDefinition;

  public InvalidExchangeRateQueryException(ErrorDefinition errorDefinition) {
    super(errorDefinition.getDescription());
    this.errorDefinition = errorDefinition;
  }
}
