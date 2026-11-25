package com.tychewealth.error.exception;

import com.tychewealth.error.ErrorDefinition;
import lombok.Getter;

/** Raised when stored exchange-rate observations cannot form a consistent cross rate. */
@Getter
public class ExchangeRateDataInconsistencyException extends RuntimeException {

  private final ErrorDefinition errorDefinition;

  public ExchangeRateDataInconsistencyException(
      ErrorDefinition errorDefinition, Object... descriptionArguments) {
    super(errorDefinition.getDescription().formatted(descriptionArguments));
    this.errorDefinition = errorDefinition;
  }
}
