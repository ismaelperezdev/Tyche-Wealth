package com.tychewealth.service;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import java.util.List;

/** Reads exchange rates for requested currency pairs. */
public interface ExchangeRateReadService {

  List<ExchangeRateResponseDto> retrieveExchangeRates(List<CurrencyPairDto> pairs);
}
