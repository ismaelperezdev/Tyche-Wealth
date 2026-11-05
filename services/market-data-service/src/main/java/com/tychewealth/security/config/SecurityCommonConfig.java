package com.tychewealth.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.ErrorResponse;
import com.tychewealth.security.JwtAuthenticationFilter;
import com.tychewealth.security.token.TokenValidator;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Shared security components used by MDS filter chains. */
@Configuration
public class SecurityCommonConfig {

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      TokenValidator tokenValidator, AuthenticationEntryPoint authenticationEntryPoint) {
    return new JwtAuthenticationFilter(tokenValidator, authenticationEntryPoint);
  }

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
      JwtAuthenticationFilter jwtAuthenticationFilter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(jwtAuthenticationFilter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, exception) ->
        writeErrorResponse(
            response, objectMapper, HttpStatus.UNAUTHORIZED, ErrorDefinition.GENERIC_UNAUTHORIZED);
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, exception) ->
        writeErrorResponse(
            response, objectMapper, HttpStatus.FORBIDDEN, ErrorDefinition.GENERIC_FORBIDDEN);
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
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
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
