package com.tychewealth.config;

import com.tychewealth.config.support.TestSupportConfig;
import com.tychewealth.service.ratelimit.RateLimitStore;
import com.tychewealth.service.ratelimit.RedisRateLimitStore;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import(TestSupportConfig.class)
public class RedisIntegrationTestConfig {

  @Bean
  @Primary
  public RateLimitStore rateLimitStore(RedisTemplate<String, String> redisTemplate) {
    return new RedisRateLimitStore(redisTemplate);
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestSupportConfig.applyCommonProperties(applicationContext);
      TestPropertyValues.of(
              "app.auth.register-rate-limit.max-requests=2",
              "app.auth.register-rate-limit.window-seconds=300",
              "app.auth.login-rate-limit.max-requests=20",
              "app.auth.login-rate-limit.window-seconds=60",
              "app.auth.refresh-rate-limit.max-requests=20",
              "app.auth.refresh-rate-limit.window-seconds=60")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
