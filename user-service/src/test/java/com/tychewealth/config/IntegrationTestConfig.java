package com.tychewealth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tychewealth.security.SecurityTestConfig;
import com.tychewealth.service.ratelimit.RateLimitStore;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@Import({SecurityTestConfig.class, TestDatabaseConfig.class})
public class IntegrationTestConfig {

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return objectMapper;
  }

  @Bean
  @Primary
  public RateLimitStore rateLimitStore() {
    return new InMemoryRateLimitStore(Clock.systemUTC());
  }
}
