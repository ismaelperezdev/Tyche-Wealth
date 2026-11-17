package com.tychewealth.config.security;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

/** Security chain for internal service-token endpoints. */
@Configuration
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
  public OAuth2TokenCustomizer<JwtEncodingContext> serviceTokenClaimsCustomizer() {
    return context -> {
      if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())
          || !OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        return;
      }

      Object configuredAudiences =
          context
              .getRegisteredClient()
              .getClientSettings()
              .getSetting(SERVICE_TOKEN_AUDIENCES_SETTING);
      if (!(configuredAudiences instanceof List<?> audiences)
          || audiences.isEmpty()
          || audiences.stream()
              .anyMatch(value -> !(value instanceof String audience) || audience.isBlank())) {
        // Missing or malformed server configuration must never produce a usable token.
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
      }
      context.getClaims().audience(audiences.stream().map(String.class::cast).toList());
    };
  }
}
