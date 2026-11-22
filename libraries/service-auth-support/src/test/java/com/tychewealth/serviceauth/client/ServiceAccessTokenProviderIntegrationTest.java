package com.tychewealth.serviceauth.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.tychewealth.serviceauth.ServiceAuthTestApplication;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    classes = ServiceAuthTestApplication.class,
    properties = {
      "spring.security.oauth2.client.registration.market-data.provider=internal",
      "spring.security.oauth2.client.registration.market-data.client-id=portfolio-service",
      "spring.security.oauth2.client.registration.market-data.client-secret=test-secret",
      "spring.security.oauth2.client.registration.market-data.authorization-grant-type="
          + "client_credentials",
      "spring.security.oauth2.client.registration.market-data.scope=market-prices:read",
      "spring.security.oauth2.client.registration.market-data.client-authentication-method="
          + "client_secret_basic"
    })
class ServiceAccessTokenProviderIntegrationTest {

  private static final AtomicInteger TOKEN_REQUESTS = new AtomicInteger();
  private static final AtomicReference<String> AUTHORIZATION_HEADER = new AtomicReference<>();
  private static final AtomicReference<String> REQUEST_BODY = new AtomicReference<>();
  private static HttpServer tokenServer;

  @Autowired private ServiceAccessTokenProvider tokenProvider;

  @DynamicPropertySource
  static void registerTokenEndpoint(DynamicPropertyRegistry registry) {
    startTokenServer();
    registry.add(
        "spring.security.oauth2.client.provider.internal.token-uri",
        () -> "http://localhost:" + tokenServer.getAddress().getPort() + "/oauth2/token");
  }

  @Test
  void requestsAndReusesClientCredentialsTokenFromTokenEndpoint() {
    assertThat(tokenProvider.getAccessToken("market-data", "portfolio-service"))
        .isEqualTo("integration-token");
    assertThat(tokenProvider.getAccessToken("market-data", "portfolio-service"))
        .isEqualTo("integration-token");

    assertThat(TOKEN_REQUESTS).hasValue(1);
    assertThat(AUTHORIZATION_HEADER.get()).startsWith("Basic ");
    assertThat(REQUEST_BODY.get()).contains("grant_type=client_credentials");
    assertThat(REQUEST_BODY.get()).contains("scope=market-prices%3Aread");
  }

  @AfterAll
  static void stopTokenServer() {
    if (tokenServer != null) {
      tokenServer.stop(0);
    }
  }

  private static void startTokenServer() {
    try {
      tokenServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      byte[] tokenResponse;
      try (var responseResource =
          Objects.requireNonNull(
              ServiceAccessTokenProviderIntegrationTest.class.getResourceAsStream(
                  "/oauth/token-response.json"),
              "OAuth token response fixture must exist")) {
        tokenResponse = responseResource.readAllBytes();
      }
      tokenServer.createContext(
          "/oauth2/token",
          exchange -> {
            TOKEN_REQUESTS.incrementAndGet();
            AUTHORIZATION_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
            REQUEST_BODY.set(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, tokenResponse.length);
            try (var responseBody = exchange.getResponseBody()) {
              responseBody.write(tokenResponse);
            }
          });
      tokenServer.start();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not start OAuth token test endpoint", exception);
    }
  }
}
