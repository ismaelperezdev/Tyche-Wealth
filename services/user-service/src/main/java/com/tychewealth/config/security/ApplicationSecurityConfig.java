package com.tychewealth.config.security;

import com.tychewealth.config.JwtAuthenticationFilter;
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

@Configuration
public class ApplicationSecurityConfig {

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource,
      @Value("${app.security.docs-public:false}") boolean docsPublic,
      @Value("${app.security.hsts.include-sub-domains:true}") boolean hstsIncludeSubDomains,
      @Value("${app.security.hsts.max-age-seconds:31536000}") long hstsMaxAgeSeconds)
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
                        hsts ->
                            hsts.includeSubDomains(hstsIncludeSubDomains)
                                .maxAgeInSeconds(hstsMaxAgeSeconds)))
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
}
