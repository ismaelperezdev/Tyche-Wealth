package com.tychewealth.security.config;

import static com.tychewealth.constants.LogConstants.AUTHENTICATION_ACTION;
import static com.tychewealth.constants.LogConstants.AUTHENTICATION_FAILURE;
import static com.tychewealth.constants.LogConstants.AUTHENTICATION_SERVICE_FAILURE;
import static com.tychewealth.constants.LogConstants.AUTHORIZATION_ACTION;
import static com.tychewealth.constants.LogConstants.AUTHORIZATION_FAILURE;
import static com.tychewealth.constants.LogConstants.SECURITY;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Shared security components used by MDS filter chains. */
@Configuration
@Slf4j
public class SecurityCommonConfig {

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, exception) -> {
      log.warn(
          AUTHENTICATION_FAILURE,
          SECURITY,
          AUTHENTICATION_ACTION,
          exception.getClass().getSimpleName());
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
      writeErrorResponse(
          response, objectMapper, HttpStatus.UNAUTHORIZED, ErrorDefinition.GENERIC_UNAUTHORIZED);
    };
  }

  @Bean
  public AuthenticationFailureHandler bearerAuthenticationFailureHandler(
      ObjectMapper objectMapper, AuthenticationEntryPoint authenticationEntryPoint) {
    return (request, response, exception) -> {
      if (exception instanceof AuthenticationServiceException) {
        log.error(AUTHENTICATION_SERVICE_FAILURE, SECURITY, AUTHENTICATION_ACTION);
        writeErrorResponse(
            response,
            objectMapper,
            HttpStatus.SERVICE_UNAVAILABLE,
            ErrorDefinition.MARKET_AUTHENTICATION_SERVICE_UNAVAILABLE);
        return;
      }
      authenticationEntryPoint.commence(request, response, (AuthenticationException) exception);
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, exception) -> {
      log.warn(AUTHORIZATION_FAILURE, SECURITY, AUTHORIZATION_ACTION);
      writeErrorResponse(
          response, objectMapper, HttpStatus.FORBIDDEN, ErrorDefinition.GENERIC_FORBIDDEN);
    };
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.security.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
    if (allowedOrigins == null || allowedOrigins.isBlank()) {
      throw new IllegalStateException("app.security.cors.allowed-origins must not be empty");
    }
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    if (origins.isEmpty() || origins.contains("*")) {
      throw new IllegalStateException("CORS origins must be valid and cannot contain '*'");
    }

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private void writeErrorResponse(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      HttpStatus status,
      ErrorDefinition definition)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    objectMapper.writeValue(
        response.getWriter(),
        ErrorResponse.builder()
            .code(definition.getCode())
            .type(definition.getType())
            .description(definition.getDescription())
            .build());
  }
}
