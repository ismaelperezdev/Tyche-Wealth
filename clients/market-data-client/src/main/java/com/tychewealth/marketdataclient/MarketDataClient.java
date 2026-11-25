package com.tychewealth.marketdataclient;

import com.tychewealth.marketdataclient.dto.ExchangeRate;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/** HTTP client for the internal market-data price endpoints. */
public class MarketDataClient {

  private static final String LATEST_PRICES_PATH = "/internal/prices/latest";
  private static final String PRICE_HISTORY_PATH = "/internal/prices/history";
  private static final String SAMPLED_PRICE_HISTORY_PATH = PRICE_HISTORY_PATH + "/sampled";
  private static final String EXCHANGE_RATES_PATH = "/internal/exchange-rates/retrieve";
  private static final String SYMBOL_IN_QUERY_PARAM = "symbol.in";
  private static final String BASE_CURRENCY_QUERY_PARAM = "baseCurrency";
  private static final String QUOTE_CURRENCY_QUERY_PARAM = "quoteCurrency";
  private static final String CAPTURED_AT_GTE_QUERY_PARAM = "capturedAt.gte";
  private static final String CAPTURED_AT_LTE_QUERY_PARAM = "capturedAt.lte";
  private static final String OFFSET_QUERY_PARAM = "offset";
  private static final String LIMIT_QUERY_PARAM = "limit";
  private static final String TOTAL_COUNT_HEADER = "X-Total-Count";

  private final RestClient restClient;

  public MarketDataClient(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
  }

  /** Gets a page of latest prices for the requested symbols. */
  public MarketDataPage<LatestMarketPrice> getLatestPrices(
      List<String> symbols, int offset, int limit, String accessToken) {
    ResponseEntity<List<LatestMarketPrice>> response =
        restClient
            .get()
            .uri(
                uriBuilder -> {
                  uriBuilder.path(LATEST_PRICES_PATH);
                  if (symbols != null) {
                    uriBuilder.queryParam(SYMBOL_IN_QUERY_PARAM, String.join(",", symbols));
                  }
                  return uriBuilder
                      .queryParam(OFFSET_QUERY_PARAM, offset)
                      .queryParam(LIMIT_QUERY_PARAM, limit)
                      .build();
                })
            .headers(headers -> addBearerToken(headers, accessToken))
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});

    return toPage(response);
  }

  /** Gets a page of price checkpoints for the requested symbols and inclusive time range. */
  public MarketDataPage<PriceCheckpoint> getPriceHistory(
      List<String> symbols, Instant from, Instant to, int offset, int limit, String accessToken) {
    ResponseEntity<List<PriceCheckpoint>> response =
        restClient
            .get()
            .uri(
                uriBuilder -> {
                  uriBuilder.path(PRICE_HISTORY_PATH);
                  if (symbols != null) {
                    uriBuilder.queryParam(SYMBOL_IN_QUERY_PARAM, String.join(",", symbols));
                  }
                  if (from != null) {
                    uriBuilder.queryParam(CAPTURED_AT_GTE_QUERY_PARAM, from);
                  }
                  if (to != null) {
                    uriBuilder.queryParam(CAPTURED_AT_LTE_QUERY_PARAM, to);
                  }
                  return uriBuilder
                      .queryParam(OFFSET_QUERY_PARAM, offset)
                      .queryParam(LIMIT_QUERY_PARAM, limit)
                      .build();
                })
            .headers(headers -> addBearerToken(headers, accessToken))
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});

    return toPage(response);
  }

  /** Gets the complete, sampled history for chart consumers without pagination metadata. */
  public List<PriceCheckpoint> getSampledPriceHistory(
      List<String> symbols, Instant from, Instant to, String accessToken) {
    return restClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder.path(SAMPLED_PRICE_HISTORY_PATH);
              if (symbols != null) {
                uriBuilder.queryParam(SYMBOL_IN_QUERY_PARAM, String.join(",", symbols));
              }
              if (from != null) {
                uriBuilder.queryParam(CAPTURED_AT_GTE_QUERY_PARAM, from);
              }
              if (to != null) {
                uriBuilder.queryParam(CAPTURED_AT_LTE_QUERY_PARAM, to);
              }
              return uriBuilder.build();
            })
        .headers(headers -> addBearerToken(headers, accessToken))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  /** Gets exchange rates for currency pairs at matching positions in the input lists. */
  public List<ExchangeRate> getExchangeRates(
      List<String> baseCurrencies, List<String> quoteCurrencies, String accessToken) {
    return restClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(EXCHANGE_RATES_PATH)
                    .queryParam(BASE_CURRENCY_QUERY_PARAM, String.join(",", baseCurrencies))
                    .queryParam(QUOTE_CURRENCY_QUERY_PARAM, String.join(",", quoteCurrencies))
                    .build())
        .headers(headers -> addBearerToken(headers, accessToken))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  private static void addBearerToken(HttpHeaders headers, String accessToken) {
    if (accessToken != null && !accessToken.isBlank()) {
      headers.setBearerAuth(accessToken);
    }
  }

  private static <T> MarketDataPage<T> toPage(ResponseEntity<List<T>> response) {
    String totalCount = response.getHeaders().getFirst(TOTAL_COUNT_HEADER);
    if (totalCount == null) {
      throw new IllegalStateException("Market-data response is missing X-Total-Count");
    }
    return new MarketDataPage<>(response.getBody(), Long.parseLong(totalCount));
  }
}
