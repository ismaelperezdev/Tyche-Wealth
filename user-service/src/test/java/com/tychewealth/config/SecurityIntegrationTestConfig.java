package com.tychewealth.config;

import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_USERNAME;

import com.tychewealth.config.support.TestSupportConfig;
import com.tychewealth.service.ratelimit.RateLimitStore;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import java.time.Clock;
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
@Import(TestSupportConfig.class)
public class SecurityIntegrationTestConfig {

  @Bean
  @Primary
  public RateLimitStore rateLimitStore() {
    return new InMemoryRateLimitStore(Clock.systemUTC());
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestSupportConfig.applyCommonProperties(applicationContext);
      TestPropertyValues.of(
              "app.security.prometheus.username=" + TEST_PROMETHEUS_USERNAME,
              "PROMETHEUS_PASSWORD=" + TEST_PROMETHEUS_PASSWORD,
              "app.auth.register-rate-limit.max-requests=2",
              "app.auth.register-rate-limit.window-seconds=300",
              "app.auth.login-rate-limit.max-requests=2",
              "app.auth.login-rate-limit.window-seconds=60",
              "app.auth.refresh-rate-limit.max-requests=2",
              "app.auth.refresh-rate-limit.window-seconds=60")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
