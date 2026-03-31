package com.tychewealth.config.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
  SecurityCommonTestConfig.class,
  ApplicationSecurityTestConfig.class,
  PrometheusSecurityTestConfig.class
})
public class SecurityTestConfig {}
