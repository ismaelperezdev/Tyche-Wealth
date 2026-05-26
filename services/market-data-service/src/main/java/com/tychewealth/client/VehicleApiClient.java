package com.tychewealth.client;

import static com.tychewealth.constants.LogConstants.FETCH_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_FAILURE;
import static com.tychewealth.constants.LogConstants.REQUEST_RETRY;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.RESOURCES_CLIENT;

import com.tychewealth.config.properties.ResourcesClientProperties;
import com.tychewealth.dto.ExternalVehicleDto;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
public class VehicleApiClient {

  private final WebClient webClient;
  private final ResourcesClientProperties properties;

  public VehicleApiClient(
      WebClient.Builder webClientBuilder, ResourcesClientProperties properties) {
    this.webClient =
        webClientBuilder
            .baseUrl(properties.baseUrl())
            .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
            .build();
    this.properties = properties;
  }

  public Mono<List<ExternalVehicleDto>> fetchResources() {
    log.debug(REQUEST_START, RESOURCES_CLIENT, FETCH_ACTION);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(properties.resourcesPath())
                    .queryParam("lowerLeftLatLon", properties.lowerLeftLatLon())
                    .queryParam("upperRightLatLon", properties.upperRightLatLon())
                    .queryParam("companyZoneIds", companyZoneIds())
                    .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(ExternalVehicleDto.class)
        .timeout(properties.requestTimeout())
        .collectList()
        .retryWhen(
            Retry.backoff(properties.maxRetries(), properties.retryBackoff())
                .filter(this::isRetryableError)
                .doBeforeRetry(
                    retrySignal ->
                        log.warn(
                            REQUEST_RETRY,
                            RESOURCES_CLIENT,
                            FETCH_ACTION,
                            retrySignal.totalRetries() + 1,
                            retrySignal.failure().getMessage())))
        .doOnSuccess(
            resources ->
                log.debug(
                    REQUEST_SUCCESS,
                    RESOURCES_CLIENT,
                    FETCH_ACTION,
                    resources == null ? 0 : resources.size()))
        .doOnError(error -> log.warn(REQUEST_FAILURE, RESOURCES_CLIENT, FETCH_ACTION, error));
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

  private String companyZoneIds() {
    return properties.companyZoneIds().stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }
}
