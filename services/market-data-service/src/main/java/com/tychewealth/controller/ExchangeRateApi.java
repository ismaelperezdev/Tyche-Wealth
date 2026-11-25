package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.EXCHANGE_RATES_RETRIEVE_URL;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Defines the internal API for retrieving exchange rates in batches. */
@Tag(name = "Exchange Rate")
public interface ExchangeRateApi {

  /** Retrieves rates for currency pairs at matching positions in the input lists. */
  @GetMapping(value = EXCHANGE_RATES_RETRIEVE_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<ExchangeRateResponseDto>> retrieveExchangeRates(
      @RequestParam("baseCurrency") List<String> baseCurrencies,
      @RequestParam("quoteCurrency") List<String> quoteCurrencies);
}
