package com.tychewealth.security.config;

import static com.tychewealth.constants.LogConstants.AUTHORIZATION_ACTION;
import static com.tychewealth.constants.LogConstants.AUTHORIZATION_SUCCESS;
import static com.tychewealth.constants.LogConstants.SECURITY;

import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

/** Security policy for the market-data API endpoints. */
@Configuration
public class ApplicationSecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(ApplicationSecurityConfig.class);

  @Bean
  @Order(2)
  public SecurityFilterChain applicationSecurityFilterChain(
      HttpSecurity http,
      AuthenticationEntryPoint authenticationEntryPoint,
      AuthenticationFailureHandler bearerAuthenticationFailureHandler,
      AccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource,
      @Value("${app.security.hsts.include-sub-domains:true}") boolean hstsIncludeSubDomains,
      @Value("${app.security.hsts.max-age-seconds:31536000}") long hstsMaxAgeSeconds)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .headers(
            headers -> configureSecurityHeaders(headers, hstsIncludeSubDomains, hstsMaxAgeSeconds))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/internal/prices/**")
                    .access(
                        (authenticationSupplier, ignored) -> {
                          var authentication = authenticationSupplier.get();
                          boolean hasRequiredScope =
                              authentication.getAuthorities().stream()
                                  .anyMatch(
                                      authority ->
                                          "SCOPE_market-prices:read"
                                              .equals(authority.getAuthority()));
                          if (hasRequiredScope) {
                            log.info(AUTHORIZATION_SUCCESS, SECURITY, AUTHORIZATION_ACTION);
                          }
                          return new AuthorizationDecision(hasRequiredScope);
                        })
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .withObjectPostProcessor(
                        new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                          @Override
                          public BearerTokenAuthenticationFilter postProcess(
                              BearerTokenAuthenticationFilter filter) {
                            filter.setAuthenticationFailureHandler(
                                bearerAuthenticationFailureHandler);
                            return filter;
                          }
                        })
                    .jwt(Customizer.withDefaults()));

    return http.build();
  }

  private void configureSecurityHeaders(
      HeadersConfigurer<HttpSecurity> headers, boolean includeSubDomains, long maxAgeSeconds) {
    headers
        .contentTypeOptions(Customizer.withDefaults())
        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
        .referrerPolicy(
            referrerPolicy ->
                referrerPolicy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
        .httpStrictTransportSecurity(
            hsts -> hsts.includeSubDomains(includeSubDomains).maxAgeInSeconds(maxAgeSeconds));
  }
}
