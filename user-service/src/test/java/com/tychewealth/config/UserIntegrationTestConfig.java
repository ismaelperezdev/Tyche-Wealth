package com.tychewealth.config;

import com.tychewealth.controller.impl.UserApiController;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.AuthTokenHelper;
import com.tychewealth.service.impl.UserServiceImpl;
import com.tychewealth.service.monitoring.AuthMetrics;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(IntegrationTestConfig.class)
@ComponentScan(
    basePackages = "com.tychewealth",
    useDefaultFilters = false,
    includeFilters = {
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UserApiController.class),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UserServiceImpl.class),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthTokenHelper.class),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthMetrics.class),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ErrorHandler.class),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UserMapper.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = AuthRefreshTokenHelper.class)
    })
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
@EntityScan(basePackageClasses = UserEntity.class)
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
