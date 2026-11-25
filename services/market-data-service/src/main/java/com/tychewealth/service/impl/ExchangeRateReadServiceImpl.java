package com.tychewealth.service.impl;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.mapper.ExchangeRateReadMapper;
import com.tychewealth.service.ExchangeRateReadService;
import com.tychewealth.service.helper.ExchangeRateQueryValidationHelper;
import com.tychewealth.service.helper.ExchangeRateReadHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Reads the latest available cross rates for batches of currency pairs. */
@Service
@RequiredArgsConstructor
public class ExchangeRateReadServiceImpl implements ExchangeRateReadService {

  private final ExchangeRateQueryValidationHelper validationHelper;
  private final ExchangeRateReadHelper readHelper;
  private final ExchangeRateReadMapper readMapper;

  @Override
  public List<ExchangeRateResponseDto> retrieveExchangeRates(
      List<String> baseCurrencies, List<String> quoteCurrencies) {
    validationHelper.validatePairs(baseCurrencies, quoteCurrencies);
    List<CurrencyPairDto> pairs = readHelper.toCurrencyPairs(baseCurrencies, quoteCurrencies);
    List<String> pivotCurrencies = readHelper.requiredPivotCurrencies(pairs);
    Map<String, ExchangeRateEntity> ratesByCurrency = readHelper.findLatestRates(pivotCurrencies);

    List<ExchangeRateResponseDto> responses = new ArrayList<>();
    for (CurrencyPairDto pair : pairs) {
      ExchangeRateEntity eurToBaseRate = ratesByCurrency.get(pair.baseCurrency());
      ExchangeRateEntity eurToQuoteRate = ratesByCurrency.get(pair.quoteCurrency());
      if (!pair.baseCurrency().equals(pair.quoteCurrency())
          && (readHelper.isRateMissing(pair.baseCurrency(), eurToBaseRate)
              || readHelper.isRateMissing(pair.quoteCurrency(), eurToQuoteRate))) {
        continue;
      }

      validationHelper.validateEffectiveDates(pair, eurToBaseRate, eurToQuoteRate);
      BigDecimal crossRate = readHelper.calculateCrossRate(pair, eurToBaseRate, eurToQuoteRate);
      LocalDate effectiveDate = readHelper.effectiveDate(pair, eurToBaseRate, eurToQuoteRate);
      responses.add(readMapper.toResponseDto(pair, crossRate, effectiveDate));
    }
    return List.copyOf(responses);
  }
}
