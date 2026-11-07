package com.tychewealth.security;

import com.tychewealth.security.config.ApplicationSecurityConfig;
import com.tychewealth.security.config.PrometheusSecurityConfig;
import com.tychewealth.security.config.SecurityCommonConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Composes the common, API, and Prometheus security configurations. */
@Configuration
@Import({
  SecurityCommonConfig.class,
  ApplicationSecurityConfig.class,
  PrometheusSecurityConfig.class
})
public class SecurityConfig {}
