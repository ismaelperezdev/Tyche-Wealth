package com.tychewealth.config;

import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_PEPPER;

import com.tychewealth.controller.impl.AuthApiController;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.TrustedDeviceEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.EmailSendResult;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapperImpl;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.ratelimit.support.AuthRateLimitSupport;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.TrustedDeviceRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.email.AuthEmailFactory;
import com.tychewealth.service.email.VerificationEmailWorkflow;
import com.tychewealth.service.email.support.EmailTemplateSupport;
import com.tychewealth.service.helper.auth.AuthForgotPasswordHelper;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthResendVerificationEmailHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.auth.AuthVerifyEmailHelper;
import com.tychewealth.service.helper.auth.AuthVerifyLoginDeviceHelper;
import com.tychewealth.service.impl.AuthServiceImpl;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.token.TokenStateStore;
import com.tychewealth.service.token.TokenValidator;
import com.tychewealth.service.token.support.AccessTokenSupport;
import com.tychewealth.service.token.support.TokenStateSupport;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import com.tychewealth.testhelper.TestRedisSupport;
import java.net.URI;
import java.time.Clock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({
  IntegrationTestConfig.class,
  AuthRateLimitConfig.class,
  AuthApiController.class,
  AuthServiceImpl.class,
  AuthValidationHelper.class,
  AuthRegisterHelper.class,
  AuthResendVerificationEmailHelper.class,
  AuthLoginHelper.class,
  AuthForgotPasswordHelper.class,
  AuthVerifyEmailHelper.class,
  AuthVerifyLoginDeviceHelper.class,
  AuthEmailFactory.class,
  VerificationEmailWorkflow.class,
  TrustedDeviceManager.class,
  AuthRefreshTokenHelper.class,
  AuthMetrics.class,
  UserMetrics.class,
  ErrorHandler.class,
  UserMapperImpl.class,
  EmailTemplateSupport.class,
  AccessTokenCodec.class,
  TokenStateStore.class,
  TokenValidator.class,
  AccessTokenSupport.class,
  TokenStateSupport.class,
  AuthRateLimitSupport.class
})
@EnableJpaRepositories(
    basePackageClasses = {
      UserRepository.class,
      RefreshTokenRepository.class,
      TrustedDeviceRepository.class
    })
@EntityScan(
    basePackageClasses = {UserEntity.class, RefreshTokenEntity.class, TrustedDeviceEntity.class})
public class AuthIntegrationTestConfig {

  @Bean
  public EmailSender emailSender() {
    EmailSender emailSender = Mockito.mock(EmailSender.class);
    Mockito.when(emailSender.send(Mockito.any())).thenReturn(EmailSendResult.DELIVERED);
    return emailSender;
  }

  @Bean
  public TestRedisSupport.InMemoryRedisState testRedisState() {
    return new TestRedisSupport.InMemoryRedisState();
  }

  @Bean
  public StringRedisTemplate stringRedisTemplate(
      TestRedisSupport.InMemoryRedisState testRedisState) {
    return TestRedisSupport.stringRedisTemplate(testRedisState);
  }

  @Bean
  public RedisTemplate<String, String> redisTemplate(
      TestRedisSupport.InMemoryRedisState testRedisState) {
    return TestRedisSupport.redisTemplate(testRedisState);
  }

  @Bean
  public Clock emailClock() {
    return Clock.systemUTC();
  }

  @Bean
  public URI verifyRegistrationUri(
      @Value("${app.auth.verify-registration-url}") String verifyRegistrationUrl) {
    return URI.create(verifyRegistrationUrl);
  }

  @Bean
  public URI verifyLoginDeviceUri(
      @Value("${app.auth.verify-login-device-url}") String verifyLoginDeviceUrl) {
    return URI.create(verifyLoginDeviceUrl);
  }

  @Bean
  public URI forgotPasswordUri(@Value("${app.auth.forgot-password-url}") String forgotPasswordUrl) {
    return URI.create(forgotPasswordUrl);
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues.of(
              "spring.liquibase.change-log=classpath:db.changelog/changelog-master.xml",
              "spring.data.redis.repositories.enabled=false",
              "app.auth.jwt.secret=4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=",
              "app.auth.jwt.refresh-token-pepper=" + TEST_REFRESH_TOKEN_PEPPER,
              "app.auth.verify-registration-url=http://localhost:8080/tyche-wealth/user-service/v1/auth/verify-registration",
              "app.auth.verify-login-device-url=http://localhost:8080/tyche-wealth/user-service/v1/auth/verify-login-device",
              "app.auth.forgot-password-url=http://localhost:3000/reset-password",
              "app.email.resend.api-key=test-resend-api-key",
              "app.email.resend.from=Tyche Wealth <auth@tyche-wealth.test>",
              "app.auth.register-rate-limit.max-requests=2",
              "app.auth.register-rate-limit.window-seconds=300",
              "app.auth.login-rate-limit.max-requests=2",
              "app.auth.login-rate-limit.window-seconds=60",
              "app.auth.forgot-password-rate-limit.max-requests=2",
              "app.auth.forgot-password-rate-limit.window-seconds=300",
              "app.auth.resend-verification-rate-limit.max-requests=2",
              "app.auth.resend-verification-rate-limit.window-seconds=300",
              "app.auth.verify-login-device-rate-limit.max-requests=2",
              "app.auth.verify-login-device-rate-limit.window-seconds=300",
              "app.auth.refresh-rate-limit.max-requests=2",
              "app.auth.refresh-rate-limit.window-seconds=60")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
