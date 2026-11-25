package com.tychewealth.error.exception;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.error.ErrorDefinition;
import java.time.LocalDate;
import lombok.Getter;

/** Raised when stored exchange-rate observations cannot form a consistent cross rate. */
@Getter
public class ExchangeRateDataInconsistencyException extends RuntimeException {

  private final ErrorDefinition errorDefinition;

  public ExchangeRateDataInconsistencyException(
      CurrencyPairDto requestedPair, LocalDate baseEffectiveDate, LocalDate quoteEffectiveDate) {
    super(
        "Cannot calculate "
            + requestedPair.baseCurrency()
            + "/"
            + requestedPair.quoteCurrency()
            + " from EUR pivot rates: EUR/"
            + requestedPair.baseCurrency()
            + " is effective on "
            + baseEffectiveDate
            + " but EUR/"
            + requestedPair.quoteCurrency()
            + " is effective on "
            + quoteEffectiveDate);
    this.errorDefinition = ErrorDefinition.EXCHANGE_RATE_DATA_INCONSISTENT;
  }
}
