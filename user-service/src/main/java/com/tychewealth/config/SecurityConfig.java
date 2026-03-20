package com.tychewealth.config;

import static com.tychewealth.constants.SecurityConstants.ACTUATOR_PROMETHEUS_PATH;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.HSTS_MAX_AGE_SECONDS;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.error.handler.ErrorResponse;
import com.tychewealth.service.monitoring.UserMetrics;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class SecurityConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain prometheusSecurityFilterChain(
      HttpSecurity http, AccessDeniedHandler accessDeniedHandler) throws Exception {
    http.securityMatcher(ACTUATOR_PROMETHEUS_PATH)
        .csrf(AbstractHttpConfigurer::disable)
        .headers(
            headers ->
                headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .referrerPolicy(
                        referrerPolicy ->
                            referrerPolicy.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("PROMETHEUS"))
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource,
      @Value("${app.security.docs-public:false}") boolean docsPublic)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .headers(
            headers ->
                headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .referrerPolicy(
                        referrerPolicy ->
                            referrerPolicy.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorize -> {
              authorize
                  .requestMatchers(
                      "/tyche-wealth/user-service/v1/auth/**",
                      "/actuator/health",
                      "/actuator/health/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll();

              if (docsPublic) {
                authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
              }

              authorize.anyRequest().authenticated();
            })
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public UserDetailsService prometheusUserDetailsService(
      @Value("${app.security.prometheus.username}") String prometheusUsername,
      @Value("${app.security.prometheus.password}") String prometheusPassword,
      PasswordEncoder passwordEncoder) {
    if (!StringUtils.hasText(prometheusUsername) || !StringUtils.hasText(prometheusPassword)) {
      throw new IllegalStateException("Prometheus username/password not configured");
    }

    UserDetails prometheusUser =
        User.withUsername(prometheusUsername)
            .password(passwordEncoder.encode(prometheusPassword))
            .roles("PROMETHEUS")
            .build();
    return new InMemoryUserDetailsManager(prometheusUser);
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      com.tychewealth.service.helper.token.TokenValidationHelper tokenValidationHelper,
      AuthenticationEntryPoint authenticationEntryPoint) {
    return new JwtAuthenticationFilter(tokenValidationHelper, authenticationEntryPoint);
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint(
      ObjectMapper objectMapper, UserMetrics userMetrics) {
    return (request, response, ex) -> {
      if (isUserRequest(request.getRequestURI())) {
        userMetrics.recordUnauthorized();
      }
      log.warn("Authentication failed for requestUri={}", request.getRequestURI(), ex);
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
    List<String> parsedAllowedOrigins = parseAllowedOrigins(allowedOrigins);

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(parsedAllowedOrigins);
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

  private List<String> parseAllowedOrigins(String allowedOrigins) {
    if (allowedOrigins == null || allowedOrigins.isBlank()) {
      throw new IllegalStateException("app.security.cors.allowed-origins must not be empty");
    }

    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();

    if (origins.isEmpty()) {
      throw new IllegalStateException(
          "app.security.cors.allowed-origins must contain at least one valid origin");
    }

    if (origins.stream().anyMatch("*"::equals)) {
      throw new IllegalStateException(
          "Wildcard CORS origin '*' is not allowed when allowCredentials is enabled");
    }

    return origins;
  }
}
