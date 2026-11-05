package com.tychewealth.security.config;

import com.tychewealth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

/** Security policy for the market-data API endpoints. */
@Configuration
public class ApplicationSecurityConfig {

  @Bean
  @Order(2)
  public SecurityFilterChain applicationSecurityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint,
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
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
