package com.tychewealth.config.security;

import static com.tychewealth.constants.SecurityConstants.ACTUATOR_PROMETHEUS_PATH;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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

@TestConfiguration
public class PrometheusSecurityTestConfig {

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
      @Value("${app.security.prometheus.username:prometheus-scraper}") String prometheusUsername,
      @Value("${app.security.prometheus.password:test-prometheus-secret}")
          String prometheusPassword,
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
}
