package com.tychewealth.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.tychewealth.config.properties.MockProviderProperties;
import com.tychewealth.config.properties.ResourcesClientProperties;
import com.tychewealth.mock.RandomVehicleTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "mock.provider.enabled", havingValue = "true")
public class MockProviderConfig {

  private static final String RANDOM_RESOURCES_TRANSFORMER_NAME =
      "random-vehicle-resources-transformer";

  @Bean(destroyMethod = "stop")
  WireMockServer embeddedMockProviderServer(
      MockProviderProperties mockProviderProperties,
      ResourcesClientProperties resourcesClientProperties) {
    validate(mockProviderProperties);

    WireMockServer wireMockServer =
        new WireMockServer(
            options()
                .port(mockProviderProperties.port())
                .extensions(new RandomVehicleTransformer(mockProviderProperties)));
    wireMockServer.start();
    wireMockServer.stubFor(
        get(urlPathEqualTo(resourcesClientProperties.resourcesPath()))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withTransformers(RANDOM_RESOURCES_TRANSFORMER_NAME)));

    log.info(
        "Embedded WireMock provider started at http://localhost:{}{}",
        mockProviderProperties.port(),
        resourcesClientProperties.resourcesPath());

    return wireMockServer;
  }

  private void validate(MockProviderProperties mockProviderProperties) {
    if (mockProviderProperties.minVehiclesPerResponse() < 1) {
      throw new IllegalArgumentException(
          "mock.provider.min-vehicles-per-response must be greater than zero");
    }

    if (mockProviderProperties.maxVehiclesPerResponse()
        < mockProviderProperties.minVehiclesPerResponse()) {
      throw new IllegalArgumentException(
          "mock.provider.max-vehicles-per-response must be greater than or equal to min-vehicles-per-response");
    }

    if (mockProviderProperties.maxVehiclesChangedPerRefresh()
        < mockProviderProperties.minVehiclesChangedPerRefresh()) {
      throw new IllegalArgumentException(
          "mock.provider.max-vehicles-changed-per-refresh must be greater than or equal to min-vehicles-changed-per-refresh");
    }

    if (mockProviderProperties.minResponseDurationSeconds() < 1) {
      throw new IllegalArgumentException(
          "mock.provider.min-response-duration-seconds must be greater than zero");
    }

    if (mockProviderProperties.maxResponseDurationSeconds()
        < mockProviderProperties.minResponseDurationSeconds()) {
      throw new IllegalArgumentException(
          "mock.provider.max-response-duration-seconds must be greater than or equal to min-response-duration-seconds");
    }
  }
}
