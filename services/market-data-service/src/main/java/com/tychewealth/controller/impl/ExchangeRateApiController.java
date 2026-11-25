package com.tychewealth.controller.impl;

import com.tychewealth.controller.ExchangeRateApi;
import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidExchangeRateQueryException;
import com.tychewealth.service.ExchangeRateReadService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for internal exchange-rate reads. */
@RestController
@RequiredArgsConstructor
public class ExchangeRateApiController implements ExchangeRateApi {

  private final ExchangeRateReadService exchangeRateReadService;

  @Override
  public ResponseEntity<List<ExchangeRateResponseDto>> retrieveExchangeRates(
      List<String> baseCurrencies, List<String> quoteCurrencies) {
    if (baseCurrencies.size() != quoteCurrencies.size()) {
      throw new InvalidExchangeRateQueryException(
          ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIR_COUNT_MISMATCH);
    }

    List<CurrencyPairDto> pairs = new ArrayList<>(baseCurrencies.size());
    IntStream.range(0, baseCurrencies.size())
        .forEach(
            index ->
                pairs.add(
                    new CurrencyPairDto(baseCurrencies.get(index), quoteCurrencies.get(index))));
    return ResponseEntity.ok(exchangeRateReadService.retrieveExchangeRates(pairs));
  }
}
