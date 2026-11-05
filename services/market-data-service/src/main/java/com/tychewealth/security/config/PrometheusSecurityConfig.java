package com.tychewealth.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.StringUtils;

/** Dedicated stateless Basic Auth chain for Prometheus metrics. */
@Configuration
public class PrometheusSecurityConfig {

  private static final String ACTUATOR_PROMETHEUS_PATH = "/actuator/prometheus";

  @Bean
  @Order(1)
  public SecurityFilterChain prometheusSecurityFilterChain(
      HttpSecurity http,
      AccessDeniedHandler accessDeniedHandler,
      @Value("${app.security.hsts.include-sub-domains:true}") boolean hstsIncludeSubDomains,
      @Value("${app.security.hsts.max-age-seconds:31536000}") long hstsMaxAgeSeconds)
      throws Exception {
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
                        hsts ->
                            hsts.includeSubDomains(hstsIncludeSubDomains)
                                .maxAgeInSeconds(hstsMaxAgeSeconds)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("PROMETHEUS"))
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler));

    return http.build();
  }

  @Bean
  public UserDetailsService prometheusUserDetailsService(
      @Value("${app.security.prometheus.username:prometheus-scraper}") String username,
      @Value("${app.security.prometheus.password}") String password,
      PasswordEncoder passwordEncoder) {
    if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
      throw new IllegalStateException("Prometheus username/password not configured");
    }
    UserDetails prometheusUser =
        User.withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("PROMETHEUS")
            .build();
    return new InMemoryUserDetailsManager(prometheusUser);
  }
}
