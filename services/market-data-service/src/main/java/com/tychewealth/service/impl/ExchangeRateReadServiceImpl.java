package com.tychewealth.service.impl;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.service.ExchangeRateReadService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Service implementation placeholder for batched exchange-rate retrieval. */
@Service
public class ExchangeRateReadServiceImpl implements ExchangeRateReadService {

  @Override
  public List<ExchangeRateResponseDto> retrieveExchangeRates(List<CurrencyPairDto> pairs) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Exchange-rate retrieval is not implemented yet");
  }
}
