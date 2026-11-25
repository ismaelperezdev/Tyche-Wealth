package com.tychewealth.service.helper;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Normalizes requested pairs and calculates exchange rates using EUR as the pivot. */
@Component
@RequiredArgsConstructor
public class ExchangeRateReadHelper {

  private static final String EUR_CURRENCY = "EUR";
  private static final BigDecimal IDENTITY_RATE = BigDecimal.ONE;

  private final ExchangeRateRepository exchangeRateRepository;

  public List<CurrencyPairDto> toCurrencyPairs(
      List<String> baseCurrencies, List<String> quoteCurrencies) {
    LinkedHashSet<CurrencyPairDto> pairs = new LinkedHashSet<>();
    for (int index = 0; index < baseCurrencies.size(); index++) {
      pairs.add(
          new CurrencyPairDto(
              baseCurrencies.get(index).trim().toUpperCase(Locale.ROOT),
              quoteCurrencies.get(index).trim().toUpperCase(Locale.ROOT)));
    }
    return List.copyOf(pairs);
  }

  public List<String> requiredPivotCurrencies(List<CurrencyPairDto> pairs) {
    LinkedHashSet<String> currencies = new LinkedHashSet<>();
    for (CurrencyPairDto pair : pairs) {
      if (!pair.baseCurrency().equals(pair.quoteCurrency())) {
        addPivotCurrency(currencies, pair.baseCurrency());
        addPivotCurrency(currencies, pair.quoteCurrency());
      }
    }
    return List.copyOf(currencies);
  }

  public Map<String, ExchangeRateEntity> findLatestRates(List<String> pivotCurrencies) {
    if (pivotCurrencies.isEmpty()) {
      return Map.of();
    }

    return exchangeRateRepository
        .findLatestEuroRates(pivotCurrencies.toArray(String[]::new))
        .stream()
        .collect(Collectors.toMap(rate -> rate.getId().getQuoteCurrency(), Function.identity()));
  }

  public boolean isRateMissing(String currency, ExchangeRateEntity rate) {
    return !EUR_CURRENCY.equals(currency) && rate == null;
  }

  public LocalDate effectiveDate(
      CurrencyPairDto pair, ExchangeRateEntity eurToBaseRate, ExchangeRateEntity eurToQuoteRate) {
    if (pair.baseCurrency().equals(pair.quoteCurrency())) {
      return null;
    }
    return eurToBaseRate == null
        ? eurToQuoteRate.getId().getEffectiveDate()
        : eurToBaseRate.getId().getEffectiveDate();
  }

  public BigDecimal calculateCrossRate(
      CurrencyPairDto requestedPair,
      ExchangeRateEntity eurToBaseRate,
      ExchangeRateEntity eurToQuoteRate) {
    if (requestedPair.baseCurrency().equals(requestedPair.quoteCurrency())) {
      return IDENTITY_RATE;
    }

    BigDecimal baseRate = rateFromEuro(requestedPair.baseCurrency(), eurToBaseRate);
    BigDecimal quoteRate = rateFromEuro(requestedPair.quoteCurrency(), eurToQuoteRate);
    return quoteRate.divide(baseRate, MathContext.DECIMAL128);
  }

  private BigDecimal rateFromEuro(String currency, ExchangeRateEntity rate) {
    return EUR_CURRENCY.equals(currency) ? IDENTITY_RATE : rate.getRate();
  }

  private void addPivotCurrency(LinkedHashSet<String> currencies, String currency) {
    if (!EUR_CURRENCY.equals(currency)) {
      currencies.add(currency);
    }
  }
}
