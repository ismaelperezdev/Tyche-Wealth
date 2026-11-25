package com.tychewealth.client;

import com.tychewealth.client.dto.FrankfurterRate;
import com.tychewealth.config.properties.FrankfurterClientProperties;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** HTTP client for Frankfurter's exchange-rate API. */
@Component
public class FrankfurterApiClient {

  private static final String RATES_PATH = "/v2/rates";
  private static final String BASE_QUERY_PARAM = "base";
  private static final String QUOTES_QUERY_PARAM = "quotes";

  private final WebClient webClient;
  private final FrankfurterClientProperties properties;

  public FrankfurterApiClient(
      WebClient.Builder webClientBuilder, FrankfurterClientProperties properties) {
    this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
    this.properties = properties;
  }

  public Mono<List<FrankfurterRate>> fetchLatestRates(
      String baseCurrency, List<String> quoteCurrencies) {
    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder.path(RATES_PATH).queryParam(BASE_QUERY_PARAM, baseCurrency);
              if (quoteCurrencies != null && !quoteCurrencies.isEmpty()) {
                uriBuilder.queryParam(QUOTES_QUERY_PARAM, String.join(",", quoteCurrencies));
              }
              return uriBuilder.build();
            })
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(FrankfurterRate.class)
        .collectList()
        .timeout(properties.requestTimeout());
  }
}
