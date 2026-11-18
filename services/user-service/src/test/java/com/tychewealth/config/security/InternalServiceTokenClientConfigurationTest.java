package com.tychewealth.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.dto.security.InternalServiceTokenPropertiesDto;
import com.tychewealth.dto.security.InternalServiceTokenPropertiesDto.ServiceClientProperties;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

class InternalServiceTokenClientConfigurationTest {

  private static final String PORTFOLIO_CLIENT_ID = "portfolio-service";
  private static final String PORTFOLIO_CLIENT_SECRET = "portfolio-secret";
  private static final String ANALYTICS_CLIENT_ID = "analytics-service";
  private static final String ANALYTICS_CLIENT_SECRET = "analytics-secret";

  @Test
  void buildsRegisteredClientsFromConfigurationWithEncodedSecretsAndPerClientPolicies() {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    InternalServiceTokenPropertiesDto properties =
        new InternalServiceTokenPropertiesDto(
            Set.of("market-data-service", "analytics-service"),
            List.of(
                new ServiceClientProperties(
                    PORTFOLIO_CLIENT_ID,
                    PORTFOLIO_CLIENT_SECRET,
                    Set.of("market-prices:read"),
                    Set.of("market-data-service"),
                    900),
                new ServiceClientProperties(
                    ANALYTICS_CLIENT_ID,
                    ANALYTICS_CLIENT_SECRET,
                    Set.of("analytics:read"),
                    Set.of("analytics-service"),
                    600)));

    RegisteredClientRepository repository =
        new InternalServiceTokenServerConfig()
            .registeredClientRepository(properties, passwordEncoder);

    RegisteredClient portfolioClient = repository.findByClientId(PORTFOLIO_CLIENT_ID);
    assertNotNull(portfolioClient);
    assertNotEquals(PORTFOLIO_CLIENT_SECRET, portfolioClient.getClientSecret());
    assertTrue(passwordEncoder.matches(PORTFOLIO_CLIENT_SECRET, portfolioClient.getClientSecret()));
    assertEquals(Set.of("market-prices:read"), portfolioClient.getScopes());
    assertEquals(
        List.of("market-data-service"),
        portfolioClient
            .getClientSettings()
            .getSetting(InternalServiceTokenServerConfig.SERVICE_TOKEN_AUDIENCES_SETTING));
    assertEquals(
        Duration.ofSeconds(900), portfolioClient.getTokenSettings().getAccessTokenTimeToLive());

    RegisteredClient analyticsClient = repository.findByClientId(ANALYTICS_CLIENT_ID);
    assertNotNull(analyticsClient);
    assertTrue(passwordEncoder.matches(ANALYTICS_CLIENT_SECRET, analyticsClient.getClientSecret()));
    assertEquals(Set.of("analytics:read"), analyticsClient.getScopes());
    assertEquals(
        Duration.ofSeconds(600), analyticsClient.getTokenSettings().getAccessTokenTimeToLive());
    assertNull(repository.findByClientId("unconfigured-service"));
  }
}
