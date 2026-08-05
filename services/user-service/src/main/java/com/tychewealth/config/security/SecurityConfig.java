package com.tychewealth.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Composes the security configuration for the user-service.
 *
 * <p>Imports the common, application API, and Prometheus metrics configurations to keep their
 * responsibilities and filter chains separate.
 */
@Configuration
@Import({
  SecurityCommonConfig.class,
  ApplicationSecurityConfig.class,
  PrometheusSecurityConfig.class
})
public class SecurityConfig {}
