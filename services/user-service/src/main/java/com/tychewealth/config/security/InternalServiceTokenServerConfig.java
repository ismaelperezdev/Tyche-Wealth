package com.tychewealth.config.security;

import com.tychewealth.dto.security.InternalServiceTokenPropertiesDto;
import com.tychewealth.dto.security.InternalServiceTokenPropertiesDto.ServiceClientProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

/** Security chain for internal service-token endpoints. */
@Configuration
@EnableConfigurationProperties(InternalServiceTokenPropertiesDto.class)
@ConditionalOnProperty(
    prefix = "app.oauth2.authorization-server",
    name = "enabled",
    havingValue = "true")
public class InternalServiceTokenServerConfig {

  /** Server-managed client setting containing a non-empty list of authorized audience names. */
  public static final String SERVICE_TOKEN_AUDIENCES_SETTING = "internal.service-token.audiences";

  @Bean
  @Order(0)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

    return http.build();
  }

  @Bean
  @ConditionalOnMissingBean(RegisteredClientRepository.class)
  public RegisteredClientRepository registeredClientRepository(
      InternalServiceTokenPropertiesDto properties, PasswordEncoder passwordEncoder) {
    List<RegisteredClient> clients =
        properties.clients().stream()
            .map(client -> buildRegisteredClient(client, passwordEncoder))
            .toList();
    return new InMemoryRegisteredClientRepository(clients);
  }

  private RegisteredClient buildRegisteredClient(
      ServiceClientProperties client, PasswordEncoder passwordEncoder) {
    return RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(client.clientId())
        .clientSecret(passwordEncoder.encode(client.clientSecret()))
        .clientName(client.clientId())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scopes(scopes -> scopes.addAll(client.scopes()))
        .clientSettings(
            ClientSettings.builder()
                .setting(SERVICE_TOKEN_AUDIENCES_SETTING, List.copyOf(client.audiences()))
                .build())
        .tokenSettings(
            TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(client.accessTokenTtlSeconds()))
                .build())
        .build();
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> serviceTokenClaimsCustomizer(
      InternalServiceTokenPropertiesDto properties) {
    return context -> {
      if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())
          || !OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        return;
      }

      if (properties.clients().stream()
          .noneMatch(
              client -> client.clientId().equals(context.getRegisteredClient().getClientId()))) {
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
      }

      Object configuredAudiences =
          context
              .getRegisteredClient()
              .getClientSettings()
              .getSetting(SERVICE_TOKEN_AUDIENCES_SETTING);
      if (!(configuredAudiences instanceof List<?> audiences)
          || audiences.isEmpty()
          || audiences.stream()
              .anyMatch(
                  value ->
                      !(value instanceof String audience)
                          || audience.isBlank()
                          || !properties.allowedAudiences().contains(audience))) {
        // Missing or malformed server configuration must never produce a usable token.
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
      }
      context.getClaims().audience(audiences.stream().map(String.class::cast).toList());
    };
  }
}
