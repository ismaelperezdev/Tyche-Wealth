package com.tychewealth.controller.impl;

import com.tychewealth.controller.ExchangeRateApi;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.service.ExchangeRateReadService;
import java.util.List;
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
    return ResponseEntity.ok(
        exchangeRateReadService.retrieveExchangeRates(baseCurrencies, quoteCurrencies));
  }
}
