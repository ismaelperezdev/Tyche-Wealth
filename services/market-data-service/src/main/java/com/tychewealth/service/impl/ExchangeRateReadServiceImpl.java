package com.tychewealth.service.impl;

import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_READ_ACTION;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_READ_COMPLETED;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_READ_SERVICE;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Reads the latest available cross rates for batches of currency pairs. */
@Service
@RequiredArgsConstructor
@Slf4j
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

      validationHelper.validatePivotRates(pair, eurToBaseRate, eurToQuoteRate);
      validationHelper.validateEffectiveDates(pair, eurToBaseRate, eurToQuoteRate);
      BigDecimal crossRate = readHelper.calculateCrossRate(pair, eurToBaseRate, eurToQuoteRate);
      LocalDate effectiveDate = readHelper.effectiveDate(pair, eurToBaseRate, eurToQuoteRate);
      responses.add(readMapper.toResponseDto(pair, crossRate, effectiveDate));
    }
    log.info(
        EXCHANGE_RATE_READ_COMPLETED,
        EXCHANGE_RATE_READ_SERVICE,
        EXCHANGE_RATE_READ_ACTION,
        pairs.size(),
        responses.size(),
        pairs.size() - responses.size());
    return List.copyOf(responses);
  }
}
