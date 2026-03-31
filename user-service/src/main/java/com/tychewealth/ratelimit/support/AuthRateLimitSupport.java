package com.tychewealth.ratelimit.support;

import static com.tychewealth.constants.ApiConstants.AUTH_FORGOT_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_RESEND_VERIFICATION_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_VERIFY_LOGIN_DEVICE_URL;

import com.tychewealth.dto.ratelimit.AuthRateLimitCallbacksDto;
import com.tychewealth.dto.ratelimit.AuthRateLimitPropertiesDto;
import com.tychewealth.dto.ratelimit.AuthRateLimitRegistrationDto;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitStore;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimitSupport {
  private static final String LOGIN_NAMESPACE = "rate-limit:auth:login";
  private static final String REGISTER_NAMESPACE = "rate-limit:auth:register";
  private static final String REFRESH_NAMESPACE = "rate-limit:auth:refresh";
  private static final String FORGOT_PASSWORD_NAMESPACE = "rate-limit:auth:forgot-password";
  private static final String RESEND_VERIFICATION_NAMESPACE = "rate-limit:auth:resend-verification";
  private static final String VERIFY_LOGIN_DEVICE_NAMESPACE = "rate-limit:auth:verify-login-device";

  private final RateLimitStore rateLimitStore;

  public AuthRateLimitSupport(RateLimitStore rateLimitStore) {
    this.rateLimitStore = rateLimitStore;
  }

  public List<AuthRateLimitRegistrationDto> buildAuthRegistrations(
      AuthRateLimitPropertiesDto properties, AuthMetrics authMetrics) {
    return List.of(
        registration(
            AUTH_REGISTER_URL,
            REGISTER_NAMESPACE,
            properties.registerRateLimit(),
            AuthRateLimitCallbacksDto.register(authMetrics)),
        registration(
            AUTH_LOGIN_URL,
            LOGIN_NAMESPACE,
            properties.loginRateLimit(),
            AuthRateLimitCallbacksDto.login(authMetrics)),
        registration(
            AUTH_FORGOT_PASSWORD_URL,
            FORGOT_PASSWORD_NAMESPACE,
            properties.forgotPasswordRateLimit(),
            AuthRateLimitCallbacksDto.none()),
        registration(
            AUTH_RESEND_VERIFICATION_URL,
            RESEND_VERIFICATION_NAMESPACE,
            properties.resendVerificationRateLimit(),
            AuthRateLimitCallbacksDto.none()),
        registration(
            AUTH_VERIFY_LOGIN_DEVICE_URL,
            VERIFY_LOGIN_DEVICE_NAMESPACE,
            properties.verifyLoginDeviceRateLimit(),
            AuthRateLimitCallbacksDto.none()));
  }

  public RateLimitInterceptor buildRefreshInterceptor(
      AuthRateLimitPropertiesDto.RateLimitDto rateLimit, AuthMetrics authMetrics) {
    Duration window = Duration.ofSeconds(rateLimit.windowSeconds());
    return new RateLimitInterceptor(
        new RateLimitInterceptorConfig(
            REFRESH_NAMESPACE,
            rateLimit.maxRequests(),
            window,
            ErrorDefinition.RATE_LIMITED.getDescription(),
            new AuthRateLimitCallbacksDto(
                ignored -> authMetrics.incrementMetric(AuthMetricEnum.REFRESH_REQUESTS),
                ignored -> authMetrics.incrementMetric(AuthMetricEnum.REFRESH_RATE_LIMITED),
                null),
            false),
        rateLimitStore);
  }

  private RateLimitInterceptor buildAuthInterceptor(
      String namespace,
      AuthRateLimitPropertiesDto.RateLimitDto rateLimit,
      AuthRateLimitCallbacksDto callbacks) {
    Duration window = Duration.ofSeconds(rateLimit.windowSeconds());
    return new RateLimitInterceptor(
        new RateLimitInterceptorConfig(
            namespace,
            rateLimit.maxRequests(),
            window,
            ErrorDefinition.RATE_LIMITED.getDescription(),
            callbacks,
            true),
        rateLimitStore);
  }

  private AuthRateLimitRegistrationDto registration(
      String pathPattern,
      String namespace,
      AuthRateLimitPropertiesDto.RateLimitDto rateLimit,
      AuthRateLimitCallbacksDto callbacks) {
    return new AuthRateLimitRegistrationDto(
        pathPattern, namespace, buildAuthInterceptor(namespace, rateLimit, callbacks));
  }

  public String refreshNamespace() {
    return REFRESH_NAMESPACE;
  }
}
