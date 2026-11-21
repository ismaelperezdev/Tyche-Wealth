package com.tychewealth.config.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@TestConfiguration
public class InternalServiceTokenTestConfig {

  @Bean
  public OAuth2AuthorizationService internalServiceTestAuthorizationService() {
    return new InMemoryOAuth2AuthorizationService();
  }

  private static final String CLIENT_ID = "portfolio-service-integration-test";
  private static final String CLIENT_SECRET = "integration-test-secret";
  private static final String MARKET_PRICES_READ_SCOPE = "market-prices:read";

  @Bean
  @Primary
  public RegisteredClientRepository internalServiceTestClientRepository(
      PasswordEncoder passwordEncoder) {
    RegisteredClient client =
        RegisteredClient.withId("portfolio-service-integration-test-registration")
            .clientId(CLIENT_ID)
            .clientSecret(passwordEncoder.encode(CLIENT_SECRET))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope(MARKET_PRICES_READ_SCOPE)
            .clientSettings(
                ClientSettings.builder()
                    .setting(
                        InternalServiceTokenServerConfig.SERVICE_TOKEN_AUDIENCES_SETTING,
                        List.of("market-data-service"))
                    .build())
            .tokenSettings(
                TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(5)).build())
            .build();

    return new InMemoryRegisteredClientRepository(client);
  }

  @Bean
  public JWKSource<SecurityContext> internalServiceTestJwkSource() throws JOSEException {
    RSAKey signingKey = new RSAKeyGenerator(2048).keyID("integration-test-key").generate();
    return new ImmutableJWKSet<>(new JWKSet(signingKey));
  }
}
