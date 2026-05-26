package com.tychewealth.config;

import static com.tychewealth.testdata.MockTestData.defaultResourcesClientProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.tychewealth.config.properties.MockProviderProperties;
import com.tychewealth.config.properties.ResourcesClientProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties({MockProviderProperties.class, ResourcesClientProperties.class})
@Import(MockProviderConfig.class)
@ActiveProfiles("test")
@SpringBootTest(
    classes = MockProviderConfigIntegrationTest.class,
    properties = "mock.provider.port=0")
class MockProviderConfigIntegrationTest {

  private final MockProviderConfig mockProviderConfig = new MockProviderConfig();

  @Autowired private WireMockServer wireMockServer;

  @Autowired private ResourcesClientProperties resourcesClientProperties;

  @Test
  void shouldExposeMockProviderEndpoint() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(wireMockServer.baseUrl() + resourcesClientProperties.resourcesPath()))
            .GET()
            .build();

    try (HttpClient httpClient = HttpClient.newHttpClient()) {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      assertThat(wireMockServer.isRunning()).isTrue();
      assertThat(response.statusCode()).isEqualTo(OK.value());
      assertThat(response.headers().firstValue(CONTENT_TYPE)).hasValue(APPLICATION_JSON_VALUE);
      assertThat(response.body()).startsWith("[");
    }
  }

  @Test
  void shouldRegisterStubForConfiguredResourcesPath() {
    assertThat(wireMockServer.listAllStubMappings().getMappings())
        .singleElement()
        .satisfies(
            stubMapping ->
                assertThat(stubMapping.getRequest().getUrlPath())
                    .isEqualTo(resourcesClientProperties.resourcesPath()));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.MockTestData#invalidMockProviderProperties")
  void shouldRejectInvalidMockProviderProperties(
      MockProviderProperties mockProviderProperties, String expectedMessage) {

    ResourcesClientProperties defaultResourcesClientProperties = defaultResourcesClientProperties();

    assertThatThrownBy(
            () ->
                mockProviderConfig.embeddedMockProviderServer(
                    mockProviderProperties, defaultResourcesClientProperties))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }
}
