package com.tychewealth.config;

import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_PEPPER;

import com.tychewealth.controller.impl.AuthApiController;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapperImpl;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.EmailService;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.email.RegisterEmailHelper;
import com.tychewealth.service.helper.email.VerificationEmailHelper;
import com.tychewealth.service.helper.token.AccessTokenHelper;
import com.tychewealth.service.helper.token.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.token.TokenStateHelper;
import com.tychewealth.service.helper.token.TokenValidationHelper;
import com.tychewealth.service.helper.token.VerificationTokenRecoveryHelper;
import com.tychewealth.service.impl.AuthServiceImpl;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.monitoring.UserMetrics;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({
  IntegrationTestConfig.class,
  EmailConfig.class,
  RefreshRateLimitConfig.class,
  AuthApiController.class,
  AuthServiceImpl.class,
  AuthValidationHelper.class,
  AuthRegisterHelper.class,
  AuthLoginHelper.class,
  RegisterEmailHelper.class,
  VerificationEmailHelper.class,
  AccessTokenHelper.class,
  TokenStateHelper.class,
  TokenValidationHelper.class,
  VerificationTokenRecoveryHelper.class,
  AuthRefreshTokenHelper.class,
  AuthMetrics.class,
  UserMetrics.class,
  ErrorHandler.class,
  UserMapperImpl.class
})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RefreshTokenRepository.class})
@EntityScan(basePackageClasses = {UserEntity.class, RefreshTokenEntity.class})
public class AuthIntegrationTestConfig {

  @org.springframework.context.annotation.Bean
  public EmailService emailService() {
    return Mockito.mock(EmailService.class);
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues.of(
              "spring.liquibase.change-log=classpath:db.changelog/changelog-master.xml",
              "app.auth.jwt.secret=4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=",
              "app.auth.jwt.refresh-token-pepper=" + TEST_REFRESH_TOKEN_PEPPER,
              "app.auth.verify-registration-url=http://localhost:8080/tyche-wealth/user-service/v1/auth/verify-registration",
              "app.email.resend.api-key=test-resend-api-key",
              "app.email.resend.from=Tyche Wealth <auth@tyche-wealth.test>",
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
