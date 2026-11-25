package com.tychewealth.client;

import static com.tychewealth.constants.TestConstants.EUR_CURRENCY;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.client.dto.FrankfurterRate;
import com.tychewealth.config.ClientIntegrationTestConfig;
import com.tychewealth.config.properties.FrankfurterClientProperties;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ActiveProfiles("test")
@SpringBootTest(classes = ClientIntegrationTestConfig.class)
class FrankfurterApiClientIntegrationTest {

  @Autowired private FrankfurterClientProperties properties;

  @Test
  void fetchesLatestRatesForBaseAndQuoteCurrencies() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    ExchangeFunction exchangeFunction =
        request -> {
          requestedUri.set(request.url());
          assertThat(request.method()).isEqualTo(HttpMethod.GET);
          assertThat(request.headers().getFirst(HttpHeaders.ACCEPT))
              .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(
                      """
                      [{"date":"2026-11-23","base":"EUR","quote":"USD","rate":1.15}]
                      """)
                  .build());
        };
    FrankfurterApiClient client =
        new FrankfurterApiClient(
            WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchLatestRates(EUR_CURRENCY, List.of(USD_CURRENCY, GBP_CURRENCY)))
        .assertNext(
            rates -> {
              assertThat(rates)
                  .containsExactly(
                      new FrankfurterRate(
                          LocalDate.parse("2026-11-23"),
                          EUR_CURRENCY,
                          USD_CURRENCY,
                          new java.math.BigDecimal("1.15")));
            })
        .verifyComplete();

    assertThat(requestedUri.get())
        .hasToString(
            "https://api.frankfurter.dev/v2/rates?base="
                + EUR_CURRENCY
                + "&quotes="
                + USD_CURRENCY
                + ","
                + GBP_CURRENCY);
  }
}
