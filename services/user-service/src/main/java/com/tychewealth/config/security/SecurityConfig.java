package com.tychewealth.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  SecurityCommonConfig.class,
  ApplicationSecurityConfig.class,
  PrometheusSecurityConfig.class
})
public class SecurityConfig {}
