package com.tychewealth.ratelimit.support;

import static com.tychewealth.constants.ApiConstants.AUTH_FORGOT_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_RESEND_VERIFICATION_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_VERIFY_LOGIN_DEVICE_URL;

import com.tychewealth.dto.auth.AuthRateLimitPropertiesDto;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitStore;
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

  public List<AuthRateLimitRegistration> buildAuthRegistrations(
      AuthRateLimitPropertiesDto properties, AuthMetrics authMetrics) {
    return List.of(
        registration(
            AUTH_REGISTER_URL,
            REGISTER_NAMESPACE,
            properties.registerRateLimit(),
            AuthRateLimitCallbacks.register(authMetrics)),
        registration(
            AUTH_LOGIN_URL,
            LOGIN_NAMESPACE,
            properties.loginRateLimit(),
            AuthRateLimitCallbacks.login(authMetrics)),
        registration(
            AUTH_FORGOT_PASSWORD_URL,
            FORGOT_PASSWORD_NAMESPACE,
            properties.forgotPasswordRateLimit(),
            AuthRateLimitCallbacks.none()),
        registration(
            AUTH_RESEND_VERIFICATION_URL,
            RESEND_VERIFICATION_NAMESPACE,
            properties.resendVerificationRateLimit(),
            AuthRateLimitCallbacks.none()),
        registration(
            AUTH_VERIFY_LOGIN_DEVICE_URL,
            VERIFY_LOGIN_DEVICE_NAMESPACE,
            properties.verifyLoginDeviceRateLimit(),
            AuthRateLimitCallbacks.none()));
  }

  public RateLimitInterceptor buildRefreshInterceptor(
      AuthRateLimitPropertiesDto.RateLimit rateLimit, AuthMetrics authMetrics) {
    return new RateLimitInterceptor(
        new RateLimitInterceptorConfig(
            REFRESH_NAMESPACE,
            rateLimit.maxRequests(),
            rateLimit.windowSeconds(),
            ErrorDefinition.RATE_LIMITED.getDescription(),
            new AuthRateLimitCallbacks(
                ignored -> authMetrics.recordRefreshRequest(),
                ignored -> authMetrics.recordRefreshRateLimited(),
                null),
            false),
        rateLimitStore);
  }

  private RateLimitInterceptor buildAuthInterceptor(
      String namespace,
      AuthRateLimitPropertiesDto.RateLimit rateLimit,
      AuthRateLimitCallbacks callbacks) {
    return new RateLimitInterceptor(
        new RateLimitInterceptorConfig(
            namespace,
            rateLimit.maxRequests(),
            rateLimit.windowSeconds(),
            ErrorDefinition.RATE_LIMITED.getDescription(),
            callbacks,
            true),
        rateLimitStore);
  }

  private AuthRateLimitRegistration registration(
      String pathPattern,
      String namespace,
      AuthRateLimitPropertiesDto.RateLimit rateLimit,
      AuthRateLimitCallbacks callbacks) {
    return new AuthRateLimitRegistration(
        pathPattern, buildAuthInterceptor(namespace, rateLimit, callbacks));
  }
}
