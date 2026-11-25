package com.tychewealth.controller.impl;

import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_API_CONTROLLER;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_QUERY_COMPLETED;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_QUERY_STARTED;
import static com.tychewealth.constants.LogConstants.EXCHANGE_RATE_READ_ACTION;

import com.tychewealth.controller.ExchangeRateApi;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.service.ExchangeRateReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for internal exchange-rate reads. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateApiController implements ExchangeRateApi {

  private final ExchangeRateReadService exchangeRateReadService;

  @Override
  public ResponseEntity<List<ExchangeRateResponseDto>> retrieveExchangeRates(
      List<String> baseCurrencies, List<String> quoteCurrencies) {
    log.info(
        EXCHANGE_RATE_QUERY_STARTED,
        EXCHANGE_RATE_API_CONTROLLER,
        EXCHANGE_RATE_READ_ACTION,
        baseCurrencies == null ? -1 : baseCurrencies.size(),
        quoteCurrencies == null ? -1 : quoteCurrencies.size());
    List<ExchangeRateResponseDto> responses =
        exchangeRateReadService.retrieveExchangeRates(baseCurrencies, quoteCurrencies);
    log.info(
        EXCHANGE_RATE_QUERY_COMPLETED,
        EXCHANGE_RATE_API_CONTROLLER,
        EXCHANGE_RATE_READ_ACTION,
        responses.size());
    return ResponseEntity.ok(responses);
  }
}
