package com.tychewealth.config.security;

import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.JwtAuthenticationFilter;
import com.tychewealth.enums.UserMetricEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.error.handler.ErrorResponse;
import com.tychewealth.monitoring.UserMetrics;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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

@TestConfiguration
public class SecurityCommonTestConfig {

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      com.tychewealth.service.token.TokenValidator tokenValidator,
      AuthenticationEntryPoint authenticationEntryPoint) {
    return new JwtAuthenticationFilter(tokenValidator, authenticationEntryPoint);
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint(
      ObjectMapper objectMapper, UserMetrics userMetrics) {
    return (request, response, ex) -> {
      if (isUserRequest(request.getRequestURI())) {
        userMetrics.incrementMetric(UserMetricEnum.UNAUTHORIZED);
      }
      writeErrorResponse(
          response, objectMapper, HttpStatus.UNAUTHORIZED, ErrorDefinition.UNAUTHORIZED);
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, ex) ->
        writeErrorResponse(response, objectMapper, HttpStatus.FORBIDDEN, ErrorDefinition.FORBIDDEN);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.security.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of(allowedOrigins.split(",")).stream().map(String::trim).toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.validateAllowCredentials();

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
    response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE);
    response.setHeader(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        ErrorResponse.builder()
            .code(definition.getCode())
            .type(definition.getType())
            .description(definition.getDescription())
            .build());
  }

  private boolean isUserRequest(String requestUri) {
    return requestUri != null && requestUri.startsWith("/tyche-wealth/user-service/v1/user");
  }
}
