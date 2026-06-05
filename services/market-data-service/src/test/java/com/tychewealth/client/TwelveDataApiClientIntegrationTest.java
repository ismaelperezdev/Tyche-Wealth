package com.tychewealth.client;

import static com.tychewealth.constants.TestConstants.RETRY_ATTEMPTS;
import static com.tychewealth.constants.TestConstants.TEST_API_KEY;
import static com.tychewealth.constants.TestConstants.TEST_BASE_URL;
import static com.tychewealth.constants.TestConstants.TEST_CONNECTION_RESET_MESSAGE;
import static com.tychewealth.constants.TestConstants.TEST_INTERVAL;
import static com.tychewealth.constants.TestConstants.TEST_META_FIELD;
import static com.tychewealth.constants.TestConstants.TEST_PRICE;
import static com.tychewealth.constants.TestConstants.TEST_PRICE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_QUOTE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL_FIELD;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_TIME_SERIES_PATH;
import static com.tychewealth.constants.TestConstants.TIMEOUT_BUFFER_MILLIS;
import static com.tychewealth.constants.TestConstants.SINGLE_ATTEMPT;
import static com.tychewealth.constants.TestConstants.SINGLE_RESULT_SIZE;
import static com.tychewealth.constants.TestConstants.TIME_SERIES_OUTPUT_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.config.ClientIntegrationTestConfig;
import com.tychewealth.config.properties.TwelveDataClientProperties;
import com.tychewealth.testdata.TwelveDataClientTestData;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
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
class TwelveDataApiClientIntegrationTest {

  @Autowired private TwelveDataClientProperties properties;

  @Test
  void shouldFetchQuoteFromConfiguredEndpoint() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          requestedUri.set(clientRequest.url());
          assertThat(clientRequest.method()).isEqualTo(HttpMethod.GET);

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(TwelveDataClientTestData.QUOTE_RESPONSE)
                  .build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchQuote(TEST_SYMBOL))
        .assertNext(
            response -> {
              assertThat(response.path(TEST_SYMBOL_FIELD).asText()).isEqualTo(TEST_SYMBOL);
              assertThat(response.path("close").asText()).isEqualTo(TEST_PRICE);
            })
        .verifyComplete();

    assertThat(requestedUri.get())
        .hasToString("%s%s?symbol=%s&apikey=%s".formatted(TEST_BASE_URL, TEST_QUOTE_PATH, TEST_SYMBOL, TEST_API_KEY));
  }

  @Test
  void shouldFetchPriceFromConfiguredEndpoint() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          requestedUri.set(clientRequest.url());
          assertThat(clientRequest.method()).isEqualTo(HttpMethod.GET);

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(TwelveDataClientTestData.PRICE_RESPONSE)
                  .build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchPrice(TEST_SYMBOL))
        .assertNext(response -> assertThat(response.path("price").asText()).isEqualTo(TEST_PRICE))
        .verifyComplete();

    assertThat(requestedUri.get())
        .hasToString("%s%s?symbol=%s&apikey=%s".formatted(TEST_BASE_URL, TEST_PRICE_PATH, TEST_SYMBOL, TEST_API_KEY));
  }

  @Test
  void shouldFetchTimeSeriesFromConfiguredEndpoint() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          requestedUri.set(clientRequest.url());
          assertThat(clientRequest.method()).isEqualTo(HttpMethod.GET);

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(TwelveDataClientTestData.TIME_SERIES_RESPONSE)
                  .build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchTimeSeries(TEST_SYMBOL, TEST_INTERVAL, TIME_SERIES_OUTPUT_SIZE))
        .assertNext(
            response -> {
              assertThat(response.path(TEST_META_FIELD).path(TEST_SYMBOL_FIELD).asText())
                  .isEqualTo(TEST_SYMBOL);
              assertThat(response.path(TEST_META_FIELD).path("interval").asText())
                  .isEqualTo(TEST_INTERVAL);
              assertThat(response.path("values")).hasSize(SINGLE_RESULT_SIZE);
            })
        .verifyComplete();

    assertThat(requestedUri.get())
        .hasToString(
            "%s%s?symbol=%s&interval=%s&outputsize=%s&apikey=%s"
                .formatted(
                    TEST_BASE_URL,
                    TEST_TIME_SERIES_PATH,
                    TEST_SYMBOL,
                    TEST_INTERVAL,
                    TIME_SERIES_OUTPUT_SIZE,
                    TEST_API_KEY));
  }

  @Test
  void shouldRetryOnceAndSucceedWhenApiReturns503() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          if (attempts.getAndIncrement() == 0) {
            return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());
          }

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(TwelveDataClientTestData.QUOTE_RESPONSE)
                  .build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchQuote(TEST_SYMBOL))
        .assertNext(
            response -> assertThat(response.path(TEST_SYMBOL_FIELD).asText()).isEqualTo(TEST_SYMBOL))
        .verifyComplete();

    assertThat(attempts.get()).isEqualTo(RETRY_ATTEMPTS);
  }

  @Test
  void shouldNotRetryWhenApiReturns400() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          attempts.incrementAndGet();
          return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchQuote(TEST_SYMBOL))
        .expectErrorSatisfies(
            error ->
                assertThat(error)
                    .hasMessageContaining(
                        "%s %s"
                            .formatted(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase())))
        .verify();

    assertThat(attempts.get()).isEqualTo(SINGLE_ATTEMPT);
  }

  @Test
  void shouldRetryWhenRequestTimesOut() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          if (attempts.getAndIncrement() == 0) {
            return Mono.delay(properties.requestTimeout().plus(Duration.ofMillis(TIMEOUT_BUFFER_MILLIS)))
                .thenReturn(
                    ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(TwelveDataClientTestData.QUOTE_RESPONSE)
                        .build());
          }

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(TwelveDataClientTestData.QUOTE_RESPONSE)
                  .build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchQuote(TEST_SYMBOL))
        .assertNext(
            response -> assertThat(response.path(TEST_SYMBOL_FIELD).asText()).isEqualTo(TEST_SYMBOL))
        .verifyComplete();

    assertThat(attempts.get()).isEqualTo(RETRY_ATTEMPTS);
  }

  @Test
  void shouldRetryWhenRequestThrowsNetworkError() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          if (attempts.getAndIncrement() == 0) {
            return Mono.error(
                new org.springframework.web.reactive.function.client.WebClientRequestException(
                    new IOException(TEST_CONNECTION_RESET_MESSAGE),
                    clientRequest.method(),
                    clientRequest.url(),
                    clientRequest.headers()));
          }

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(TwelveDataClientTestData.QUOTE_RESPONSE)
                  .build());
        };

    TwelveDataApiClient client =
        new TwelveDataApiClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);

    StepVerifier.create(client.fetchQuote(TEST_SYMBOL))
        .assertNext(
            response -> assertThat(response.path(TEST_SYMBOL_FIELD).asText()).isEqualTo(TEST_SYMBOL))
        .verifyComplete();

    assertThat(attempts.get()).isEqualTo(RETRY_ATTEMPTS);
  }
}
