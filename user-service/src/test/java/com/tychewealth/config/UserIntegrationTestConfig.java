package com.tychewealth.config;

import com.tychewealth.controller.impl.UserApiController;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.AuthTokenHelper;
import com.tychewealth.service.helper.user.UserHelper;
import com.tychewealth.service.helper.user.UserValidationHelper;
import com.tychewealth.service.impl.UserServiceImpl;
import com.tychewealth.service.monitoring.AuthMetrics;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(IntegrationTestConfig.class)
@ComponentScan(
    basePackageClasses = {
      UserApiController.class,
      UserServiceImpl.class,
      AuthTokenHelper.class,
      AuthMetrics.class,
      ErrorHandler.class,
      UserMapper.class,
      AuthRefreshTokenHelper.class,
      UserHelper.class,
      UserValidationHelper.class
    })
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RefreshTokenRepository.class})
@EntityScan(basePackageClasses = {UserEntity.class, RefreshTokenEntity.class})
public class UserIntegrationTestConfig {

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues.of(
              "spring.liquibase.change-log=classpath:db.changelog/changelog-master.xml",
              "app.auth.jwt.secret=4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
