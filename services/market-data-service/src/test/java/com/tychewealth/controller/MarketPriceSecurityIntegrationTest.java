package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.LATEST_PRICES_ENDPOINT;
import static com.tychewealth.constants.TestConstants.PRICE_HISTORY_ENDPOINT;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL_IN_FILTER;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.store.MarketQuoteStore;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
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
      "app.market-price.scheduler.enabled=false"
    })
@AutoConfigureMockMvc
class MarketPriceSecurityIntegrationTest {

  private static final String JWT_SECRET = "test-jwt-secret-0123456789abcdef";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RedisTemplate<String, String> redisTemplate;

  @MockitoBean private MarketQuoteStore marketQuoteStore;

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
        .perform(get("/actuator/prometheus").header("Authorization", basicAuth()))
        .andExpect(status().isOk());
  }

  @Test
  void allowsValidBearerTokenOnProtectedApiRoute() throws Exception {
    String token = accessToken("valid-jti");
    when(redisTemplate.hasKey("auth:access-token:blacklist:jti:valid-jti")).thenReturn(false);
    when(marketQuoteStore.findBySymbols(List.of(TEST_SYMBOL))).thenReturn(List.of());

    mockMvc
        .perform(
            get(LATEST_PRICES_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsInvalidBearerToken() throws Exception {
    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  @Test
  void rejectsRevokedBearerToken() throws Exception {
    String token = accessToken("revoked-jti");
    when(redisTemplate.hasKey("auth:access-token:blacklist:jti:revoked-jti")).thenReturn(true);

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  @Test
  void rejectsBearerTokenWhenRedisIsUnavailable() throws Exception {
    String token = accessToken("redis-unavailable-jti");
    doThrow(new IllegalStateException("Redis unavailable"))
        .when(redisTemplate)
        .hasKey("auth:access-token:blacklist:jti:redis-unavailable-jti");

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  @Test
  void basicCredentialsDoNotAuthenticateProtectedApiRoute() throws Exception {
    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).header("Authorization", basicAuth()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-602"));
  }

  private String basicAuth() {
    String credentials = "prometheus-scraper" + ":" + "test-prometheus-password";
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  private String accessToken(String tokenId) {
    SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject("42")
        .id(tokenId)
        .issuedAt(Date.from(Instant.now().minusSeconds(1)))
        .expiration(Date.from(Instant.now().plusSeconds(900)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }
}
