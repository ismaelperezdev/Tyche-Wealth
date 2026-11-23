package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.LATEST_PRICES_ENDPOINT;
import static com.tychewealth.constants.TestConstants.PRICE_HISTORY_ENDPOINT;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL_IN_FILTER;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.store.MarketQuoteStore;
import com.tychewealth.testdata.SecurityTestHelper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:market-price-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false",
      "kafka.enabled=false",
      "app.market-price.scheduler.enabled=false",
      "app.service-auth.resource-server.enabled=true",
      "app.service-auth.resource-server.issuer=http://localhost:8080",
      "app.service-auth.resource-server.jwk-set-uri=http://localhost:8080/oauth2/jwks",
      "app.service-auth.resource-server.audience=market-data-service"
    })
@AutoConfigureMockMvc
class MarketPriceSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RedisTemplate<String, String> redisTemplate;

  @MockitoBean private MarketQuoteStore marketQuoteStore;

  @MockitoBean private JwtDecoder jwtDecoder;

  @ParameterizedTest
  @ValueSource(strings = {LATEST_PRICES_ENDPOINT, PRICE_HISTORY_ENDPOINT})
  void rejectsUnauthenticatedRequestsToInternalPriceRoutes(String endpoint) throws Exception {
    mockMvc
        .perform(get(endpoint).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  @Test
  void allowsPrometheusScrapingWithConfiguredBasicCredentials() throws Exception {
    mockMvc
        .perform(
            get("/actuator/prometheus").header("Authorization", SecurityTestHelper.basicAuth()))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsPrometheusScrapingWithoutAuthorization() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }

  @Test
  void allowsValidBearerTokenOnProtectedApiRoute() throws Exception {
    String token = "valid-service-token";
    when(jwtDecoder.decode(token)).thenReturn(serviceJwt(token, "market-prices:read"));
    when(marketQuoteStore.findBySymbols(List.of(TEST_SYMBOL))).thenReturn(List.of());

    mockMvc
        .perform(
            get(LATEST_PRICES_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsHumanToken() throws Exception {
    when(jwtDecoder.decode("human-token"))
        .thenThrow(new BadJwtException("Token is not a service token"));

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer human-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  @Test
  void rejectsServiceTokenWithoutRequiredScope() throws Exception {
    String token = "service-token-without-scope";
    when(jwtDecoder.decode(token)).thenReturn(serviceJwt(token, "other-scope"));

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsInvalidBearerToken() throws Exception {
    when(jwtDecoder.decode("not-a-jwt")).thenThrow(new BadJwtException("Invalid JWT"));

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  @Test
  void returnsServiceUnavailableWhenJwksCannotBeRetrieved() throws Exception {
    when(jwtDecoder.decode("service-token"))
        .thenThrow(new JwtException("Unable to retrieve signing keys"));

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer service-token"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("TYCHE-620"))
        .andExpect(jsonPath("$.type").value("MARKET_AUTHENTICATION_SERVICE_UNAVAILABLE"));
  }

  @Test
  void basicCredentialsDoNotAuthenticateProtectedApiRoute() throws Exception {
    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT).header("Authorization", SecurityTestHelper.basicAuth()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  private static Jwt serviceJwt(String token, String scope) {
    Instant now = Instant.now();
    return Jwt.withTokenValue(token)
        .header("alg", "RS256")
        .issuer("http://localhost:8080")
        .issuedAt(now.minusSeconds(1))
        .expiresAt(now.plusSeconds(900))
        .claim("token_type", "service")
        .claim("aud", List.of("market-data-service"))
        .claim("scope", scope)
        .build();
  }
}
