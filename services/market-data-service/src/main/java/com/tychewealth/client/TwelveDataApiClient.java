package com.tychewealth.client;

import static com.tychewealth.constants.CommonConstants.API_KEY_QUERY_PARAM;
import static com.tychewealth.constants.CommonConstants.INTERVAL_QUERY_PARAM;
import static com.tychewealth.constants.CommonConstants.OUTPUT_SIZE_QUERY_PARAM;
import static com.tychewealth.constants.CommonConstants.SYMBOL_QUERY_PARAM;
import static com.tychewealth.constants.LogConstants.FETCH_ACTION;
import static com.tychewealth.constants.LogConstants.PRICE_ACTION;
import static com.tychewealth.constants.LogConstants.QUOTE_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_FAILURE;
import static com.tychewealth.constants.LogConstants.REQUEST_RETRY;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.TIME_SERIES_ACTION;
import static com.tychewealth.constants.LogConstants.TWELVE_DATA_CLIENT;

import com.fasterxml.jackson.databind.JsonNode;
import com.tychewealth.config.properties.TwelveDataClientProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Slf4j
@Component
public class TwelveDataApiClient {

  private final WebClient webClient;
  private final TwelveDataClientProperties properties;

  public TwelveDataApiClient(
      WebClient.Builder webClientBuilder, TwelveDataClientProperties properties) {
    this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
    this.properties = properties;
  }

  public Mono<JsonNode> fetchPrice(String symbol) {
    return get(
        PRICE_ACTION,
        uriBuilder ->
            uriBuilder
                .path(properties.pricePath())
                .queryParam(SYMBOL_QUERY_PARAM, symbol)
                .queryParam(API_KEY_QUERY_PARAM, properties.apiKey())
                .build());
  }

  public Mono<JsonNode> fetchQuote(String symbol) {
    return get(
        QUOTE_ACTION,
        uriBuilder ->
            uriBuilder
                .path(properties.quotePath())
                .queryParam(SYMBOL_QUERY_PARAM, symbol)
                .queryParam(API_KEY_QUERY_PARAM, properties.apiKey())
                .build());
  }

  public Mono<JsonNode> fetchTimeSeries(String symbol, String interval, int outputSize) {
    return get(
        TIME_SERIES_ACTION,
        uriBuilder ->
            uriBuilder
                .path(properties.timeSeriesPath())
                .queryParam(SYMBOL_QUERY_PARAM, symbol)
                .queryParam(INTERVAL_QUERY_PARAM, interval)
                .queryParam(OUTPUT_SIZE_QUERY_PARAM, outputSize)
                .queryParam(API_KEY_QUERY_PARAM, properties.apiKey())
                .build());
  }

  private Mono<JsonNode> get(
      String action,
      java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI>
          uriCustomizer) {
    log.debug(REQUEST_START, TWELVE_DATA_CLIENT, FETCH_ACTION + action);

    return webClient
        .get()
        .uri(uriCustomizer)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(properties.requestTimeout())
        .retryWhen(retrySpec(action))
        .doOnSuccess(
            response ->
                log.debug(
                    REQUEST_SUCCESS,
                    TWELVE_DATA_CLIENT,
                    FETCH_ACTION + action,
                    response == null ? 0 : 1))
        .doOnError(
            error -> log.warn(REQUEST_FAILURE, TWELVE_DATA_CLIENT, FETCH_ACTION + action, error));
  }

  private Retry retrySpec(String action) {
    RetryBackoffSpec fallbackRetry =
        Retry.backoff(properties.maxRetries(), properties.retryBackoff())
            .filter(this::isRetryableError);

    return Retry.from(
        retrySignals ->
            retrySignals.concatMap(retrySignal -> retryDelay(retrySignal, action, fallbackRetry)));
  }

  private boolean isRetryableError(Throwable error) {
    if (error instanceof TimeoutException || error instanceof WebClientRequestException) {
      return true;
    }

    if (error instanceof WebClientResponseException responseException) {
      return responseException.getStatusCode().is5xxServerError()
          || responseException.getStatusCode().value() == 429;
    }

    return false;
  }

  private Mono<Long> retryDelay(
      Retry.RetrySignal retrySignal, String action, RetryBackoffSpec fallbackRetry) {
    Throwable failure = retrySignal.failure();
    if (!isRetryableError(failure)) {
      return Mono.error(failure);
    }

    if (retrySignal.totalRetries() >= properties.maxRetries()) {
      return Mono.error(Exceptions.retryExhausted("Retries exhausted", failure));
    }

    Mono<Long> retryTrigger =
        retryAfterDelay(failure)
            .map(Mono::delay)
            .orElseGet(
                () ->
                    Mono.from(fallbackRetry.generateCompanion(Flux.just(retrySignal)))
                        .cast(Long.class));

    return retryTrigger.doOnNext(
        ignored ->
            log.warn(
                REQUEST_RETRY,
                TWELVE_DATA_CLIENT,
                FETCH_ACTION + action,
                retrySignal.totalRetries() + 1,
                failure.getMessage()));
  }

  private Optional<Duration> retryAfterDelay(Throwable failure) {
    if (!(failure instanceof WebClientResponseException responseException)
        || responseException.getStatusCode().value() != 429) {
      return Optional.empty();
    }

    return parseRetryAfter(
        responseException.getHeaders().getFirst(HttpHeaders.RETRY_AFTER), Instant.now());
  }

  static Optional<Duration> parseRetryAfter(String retryAfterHeader, Instant now) {
    if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
      return Optional.empty();
    }

    String trimmedHeader = retryAfterHeader.trim();
    try {
      long delaySeconds = Long.parseLong(trimmedHeader);
      return Optional.of(Duration.ofSeconds(Math.max(0L, delaySeconds)));
    } catch (NumberFormatException ignored) {
      try {
        Instant retryAt =
            ZonedDateTime.parse(trimmedHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        Duration delay = Duration.between(now, retryAt);
        return Optional.of(delay.isNegative() ? Duration.ZERO : delay);
      } catch (DateTimeParseException ignoredDateParseException) {
        return Optional.empty();
      }
    }
  }
}
