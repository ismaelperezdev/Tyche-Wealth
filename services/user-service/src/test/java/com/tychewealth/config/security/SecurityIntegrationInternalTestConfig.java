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
      TestPropertyValues.of(
              "app.oauth2.authorization-server.enabled=true",
              "app.oauth2.authorization-server.allowed-audiences=market-data-service",
              "app.oauth2.authorization-server.clients[0].client-id=portfolio-service-integration-test",
              "app.oauth2.authorization-server.clients[0].client-secret=integration-test-secret",
              "app.oauth2.authorization-server.clients[0].scopes=market-prices:read",
              "app.oauth2.authorization-server.clients[0].audiences=market-data-service",
              "app.oauth2.authorization-server.clients[0].access-token-ttl-seconds=300",
              "app.oauth2.authorization-server.clients[1].client-id=other-client",
              "app.oauth2.authorization-server.clients[1].client-secret=integration-test-secret",
              "app.oauth2.authorization-server.clients[1].scopes=market-prices:read",
              "app.oauth2.authorization-server.clients[1].audiences=market-data-service",
              "app.oauth2.authorization-server.clients[1].access-token-ttl-seconds=300")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
