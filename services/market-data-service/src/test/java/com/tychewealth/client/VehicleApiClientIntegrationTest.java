package com.tychewealth.client;

import static com.tychewealth.testdata.ClientTestData.RESOURCES_RESPONSE;
import static com.tychewealth.testdata.ClientTestData.defaultExternalVehicleDto;
import static com.tychewealth.testdata.ClientTestData.expectedUri;
import static com.tychewealth.testhelper.ClientTestHelper.createClient;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.config.ClientIntegrationTestConfig;
import com.tychewealth.config.properties.ResourcesClientProperties;
import com.tychewealth.dto.ExternalVehicleDto;
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
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ActiveProfiles("test")
@SpringBootTest(classes = ClientIntegrationTestConfig.class)
class VehicleApiClientIntegrationTest {

  @Autowired private ResourcesClientProperties resourcesClientProperties;

  @Test
  void shouldFetchResourcesFromConfiguredEndpoint() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    AtomicReference<String> requestedUserAgent = new AtomicReference<>();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          requestedUri.set(clientRequest.url());
          requestedUserAgent.set(clientRequest.headers().getFirst(HttpHeaders.USER_AGENT));
          assertThat(clientRequest.method()).isEqualTo(HttpMethod.GET);

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(RESOURCES_RESPONSE)
                  .build());
        };

    VehicleApiClient client = createClient(exchangeFunction, resourcesClientProperties);

    StepVerifier.create(client.fetchResources())
        .assertNext(
            resources -> {
              assertThat(resources).hasSize(1);

              ExternalVehicleDto vehicle = resources.getFirst();
              assertThat(vehicle).isEqualTo(defaultExternalVehicleDto());
            })
        .verifyComplete();

    assertThat(requestedUri.get()).hasToString(expectedUri(resourcesClientProperties));
    assertThat(requestedUserAgent.get()).isEqualTo(resourcesClientProperties.userAgent());
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
                  .body(RESOURCES_RESPONSE)
                  .build());
        };

    VehicleApiClient client = createClient(exchangeFunction, resourcesClientProperties);

    StepVerifier.create(client.fetchResources())
        .assertNext(resources -> assertThat(resources).containsExactly(defaultExternalVehicleDto()))
        .verifyComplete();

    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  void shouldNotRetryWhenApiReturns400() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          attempts.incrementAndGet();
          return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
        };

    VehicleApiClient client = createClient(exchangeFunction, resourcesClientProperties);

    StepVerifier.create(client.fetchResources())
        .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("400 Bad Request"))
        .verify();

    assertThat(attempts.get()).isEqualTo(1);
  }

  @Test
  void shouldRetryOnceAndSucceedWhenRequestThrowsNetworkError() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          if (attempts.getAndIncrement() == 0) {
            return Mono.error(
                new WebClientRequestException(
                    new IOException("Connection reset"),
                    clientRequest.method(),
                    clientRequest.url(),
                    clientRequest.headers()));
          }

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(RESOURCES_RESPONSE)
                  .build());
        };

    VehicleApiClient client = createClient(exchangeFunction, resourcesClientProperties);

    StepVerifier.create(client.fetchResources())
        .assertNext(resources -> assertThat(resources).containsExactly(defaultExternalVehicleDto()))
        .verifyComplete();

    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  void shouldReturnEmptyListWhenApiReturnsEmptyArray() {
    ExchangeFunction exchangeFunction =
        clientRequest ->
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("[]")
                    .build());

    VehicleApiClient client = createClient(exchangeFunction, resourcesClientProperties);

    StepVerifier.create(client.fetchResources())
        .assertNext(resources -> assertThat(resources).isEmpty())
        .verifyComplete();
  }

  @Test
  void shouldRetryOnceAndSucceedWhenRequestTimesOut() {
    AtomicInteger attempts = new AtomicInteger();
    ExchangeFunction exchangeFunction =
        clientRequest -> {
          if (attempts.getAndIncrement() == 0) {
            return Mono.delay(
                    resourcesClientProperties.requestTimeout().plus(Duration.ofMillis(200)))
                .thenReturn(
                    ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(RESOURCES_RESPONSE)
                        .build());
          }

          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(RESOURCES_RESPONSE)
                  .build());
        };

    VehicleApiClient client = createClient(exchangeFunction, resourcesClientProperties);

    StepVerifier.create(client.fetchResources())
        .assertNext(resources -> assertThat(resources).containsExactly(defaultExternalVehicleDto()))
        .verifyComplete();

    assertThat(attempts.get()).isEqualTo(2);
  }
}
