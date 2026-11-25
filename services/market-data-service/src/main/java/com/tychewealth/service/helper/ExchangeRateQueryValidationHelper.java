package com.tychewealth.service.helper;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.ExchangeRateDataInconsistencyException;
import com.tychewealth.error.exception.InvalidExchangeRateQueryException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Validates currency-pair lists requested from the exchange-rate read API. */
@Component
public class ExchangeRateQueryValidationHelper {

  public void validatePairs(List<String> baseCurrencies, List<String> quoteCurrencies) {
    if (baseCurrencies == null || quoteCurrencies == null) {
      throw new InvalidExchangeRateQueryException(
          ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIRS_REQUIRED);
    }
    if (baseCurrencies.size() != quoteCurrencies.size()) {
      throw new InvalidExchangeRateQueryException(
          ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIR_COUNT_MISMATCH);
    }
    if (baseCurrencies.isEmpty()) {
      throw new InvalidExchangeRateQueryException(
          ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIRS_REQUIRED);
    }

    baseCurrencies.forEach(this::validateCurrency);
    quoteCurrencies.forEach(this::validateCurrency);
  }

  public void validateEffectiveDates(
      CurrencyPairDto requestedPair,
      ExchangeRateEntity eurToBaseRate,
      ExchangeRateEntity eurToQuoteRate) {
    if (eurToBaseRate == null || eurToQuoteRate == null) {
      return;
    }

    var baseEffectiveDate = eurToBaseRate.getId().getEffectiveDate();
    var quoteEffectiveDate = eurToQuoteRate.getId().getEffectiveDate();
    if (!baseEffectiveDate.equals(quoteEffectiveDate)) {
      throw new ExchangeRateDataInconsistencyException(
          requestedPair, baseEffectiveDate, quoteEffectiveDate);
    }
  }

  private void validateCurrency(String currency) {
    if (currency == null || !currency.trim().matches("(?i)[a-z]{3}")) {
      throw new InvalidExchangeRateQueryException(ErrorDefinition.EXCHANGE_RATE_CURRENCY_INVALID);
    }
  }
}
