package com.tychewealth.config;

import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_PEPPER;

import com.tychewealth.controller.impl.UserApiController;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapperImpl;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.auth.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.user.UserHelper;
import com.tychewealth.service.helper.user.UserValidationHelper;
import com.tychewealth.service.impl.UserServiceImpl;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.token.TokenStateStore;
import com.tychewealth.service.token.TokenValidator;
import com.tychewealth.service.token.support.AccessTokenSupport;
import com.tychewealth.service.token.support.TokenStateSupport;
import com.tychewealth.testhelper.TestRedisSupport;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({
  IntegrationTestConfig.class,
  UserApiController.class,
  UserServiceImpl.class,
  AccessTokenCodec.class,
  TokenStateStore.class,
  TokenValidator.class,
  AccessTokenSupport.class,
  TokenStateSupport.class,
  AuthRefreshTokenHelper.class,
  UserMetrics.class,
  ErrorHandler.class,
  UserMapperImpl.class,
  UserHelper.class,
  UserValidationHelper.class
})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RefreshTokenRepository.class})
@EntityScan(basePackageClasses = {UserEntity.class, RefreshTokenEntity.class})
public class UserIntegrationTestConfig {

  @Bean
  public AuthMetrics authMetrics() {
    return Mockito.mock(AuthMetrics.class);
  }

  @Bean
  public RedisTemplate<String, String> redisTemplate() {
    return TestRedisSupport.redisTemplate();
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues.of(
              "spring.liquibase.change-log=classpath:db.changelog/changelog-master.xml",
              "spring.data.redis.repositories.enabled=false",
              "app.security.prometheus.username=" + TEST_PROMETHEUS_USERNAME,
              "app.security.prometheus.password=" + TEST_PROMETHEUS_PASSWORD,
              "PROMETHEUS_PASSWORD=" + TEST_PROMETHEUS_PASSWORD,
              "app.auth.jwt.secret=4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=",
              "app.auth.jwt.refresh-token-pepper=" + TEST_REFRESH_TOKEN_PEPPER,
              "app.auth.verify-registration-url=http://localhost:8080/tyche-wealth/user-service/v1/auth/verify-registration",
              "app.auth.verify-login-device-url=http://localhost:8080/tyche-wealth/user-service/v1/auth/verify-login-device",
              "app.auth.forgot-password-url=http://localhost:3000/reset-password")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
