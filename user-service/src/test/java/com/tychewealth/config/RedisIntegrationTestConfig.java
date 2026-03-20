package com.tychewealth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tychewealth.controller.impl.AuthApiController;
import com.tychewealth.controller.impl.UserApiController;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapperImpl;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.token.AccessTokenHelper;
import com.tychewealth.service.helper.token.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.token.TokenStateHelper;
import com.tychewealth.service.helper.token.TokenValidationHelper;
import com.tychewealth.service.helper.user.UserHelper;
import com.tychewealth.service.helper.user.UserValidationHelper;
import com.tychewealth.service.impl.AuthServiceImpl;
import com.tychewealth.service.impl.UserServiceImpl;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.monitoring.UserMetrics;
import com.tychewealth.service.ratelimit.RateLimitStore;
import com.tychewealth.service.ratelimit.RedisRateLimitStore;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({
  SecurityTestConfig.class,
  TestDatabaseConfig.class,
  RefreshRateLimitConfig.class,
  AuthApiController.class,
  UserApiController.class,
  AuthServiceImpl.class,
  UserServiceImpl.class,
  AuthValidationHelper.class,
  AuthRegisterHelper.class,
  AuthLoginHelper.class,
  AccessTokenHelper.class,
  TokenStateHelper.class,
  TokenValidationHelper.class,
  AuthRefreshTokenHelper.class,
  UserHelper.class,
  UserValidationHelper.class,
  AuthMetrics.class,
  UserMetrics.class,
  ErrorHandler.class,
  UserMapperImpl.class
})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RefreshTokenRepository.class})
@EntityScan(basePackageClasses = {UserEntity.class, RefreshTokenEntity.class})
public class RedisIntegrationTestConfig {

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
  public RateLimitStore rateLimitStore(RedisTemplate<String, String> redisTemplate) {
    return new RedisRateLimitStore(redisTemplate);
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues.of(
              "spring.liquibase.change-log=classpath:db.changelog/changelog-master.xml",
              "app.auth.jwt.secret=4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=",
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
