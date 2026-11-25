package com.tychewealth.service;

import com.tychewealth.client.FrankfurterApiClient;
import com.tychewealth.client.dto.FrankfurterRate;
import com.tychewealth.config.properties.ExchangeRateIngestionProperties;
import com.tychewealth.repository.ExchangeRateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Fetches Frankfurter rates and persists the returned observations. */
@Service
@RequiredArgsConstructor
public class ExchangeRateIngestionService {

  private final FrankfurterApiClient frankfurterApiClient;
  private final ExchangeRateRepository exchangeRateRepository;
  private final ExchangeRateIngestionProperties properties;

  public int ingestLatestRates() {
    List<FrankfurterRate> rates =
        frankfurterApiClient
            .fetchLatestRates(properties.baseCurrency(), properties.quoteCurrencies())
            .block();

    if (rates == null || rates.isEmpty()) {
      return 0;
    }

    return exchangeRateRepository.upsertBatch(
        rates.stream().map(FrankfurterRate::base).toArray(String[]::new),
        rates.stream().map(FrankfurterRate::quote).toArray(String[]::new),
        rates.stream().map(rate -> rate.date().toString()).toArray(String[]::new),
        rates.stream().map(rate -> rate.rate().toPlainString()).toArray(String[]::new));
  }
}
