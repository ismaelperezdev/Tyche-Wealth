package com.tychewealth.config.security;

import com.tychewealth.config.support.TestSupportConfig;
import com.tychewealth.ratelimit.RateLimitStore;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import java.time.Clock;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({TestSupportConfig.class, SecurityConfig.class, InternalServiceTokenTestConfig.class})
public class SecurityIntegrationInternalTestConfig {

  @Bean
  @Primary
  public RateLimitStore rateLimitStore() {
    return new InMemoryRateLimitStore(Clock.systemUTC());
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
      TestSupportConfig.applyCommonProperties(applicationContext);
      TestPropertyValues.of("app.oauth2.authorization-server.enabled=true")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
