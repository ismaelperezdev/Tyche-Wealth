package com.tychewealth.ratelimit;

import static com.tychewealth.constants.ApiConstants.AUTH_FORGOT_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_RESEND_VERIFICATION_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_VERIFY_LOGIN_DEVICE_URL;
import static com.tychewealth.constants.TestConstants.TEST_RATE_LIMIT_STORE_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.ratelimit.AuthRateLimitPropertiesDto;
import com.tychewealth.dto.ratelimit.AuthRateLimitRegistrationDto;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.support.AuthRateLimitSupport;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class AuthRateLimitSupportTest {

  private static final AuthRateLimitPropertiesDto DEFAULT_AUTH_RATE_LIMIT_PROPERTIES =
      new AuthRateLimitPropertiesDto(
          new AuthRateLimitPropertiesDto.RateLimitDto(10, 60),
          new AuthRateLimitPropertiesDto.RateLimitDto(5, 300),
          new AuthRateLimitPropertiesDto.RateLimitDto(10, 60),
          new AuthRateLimitPropertiesDto.RateLimitDto(2, 3600),
          new AuthRateLimitPropertiesDto.RateLimitDto(2, 3600),
          new AuthRateLimitPropertiesDto.RateLimitDto(3, 900));

  private static final String TEST_AUTH_RATE_LIMIT_LOGIN_NAMESPACE = "rate-limit:auth:login";
  private static final String TEST_AUTH_RATE_LIMIT_REGISTER_NAMESPACE = "rate-limit:auth:register";
  private static final String TEST_AUTH_RATE_LIMIT_REFRESH_NAMESPACE = "rate-limit:auth:refresh";
  private static final String TEST_AUTH_RATE_LIMIT_FORGOT_PASSWORD_NAMESPACE =
      "rate-limit:auth:forgot-password";
  private static final String TEST_AUTH_RATE_LIMIT_RESEND_VERIFICATION_NAMESPACE =
      "rate-limit:auth:resend-verification";
  private static final String TEST_AUTH_RATE_LIMIT_VERIFY_LOGIN_DEVICE_NAMESPACE =
      "rate-limit:auth:verify-login-device";

  private AuthRateLimitSupport authRateLimitSupport;
  private AuthMetrics authMetrics;

  @BeforeEach
  void setUp() {
    RateLimitStore rateLimitStore = mock(RateLimitStore.class);
    when(rateLimitStore.increment(anyString(), anyString(), any(Duration.class)))
        .thenThrow(new IllegalStateException(TEST_RATE_LIMIT_STORE_UNAVAILABLE));
    authRateLimitSupport = new AuthRateLimitSupport(rateLimitStore);
    authMetrics = new AuthMetrics(new SimpleMeterRegistry());
  }

  @Test
  void buildAuthRegistrationsReturnsExpectedEndpointRegistrations() {
    List<AuthRateLimitRegistrationDto> registrations =
        authRateLimitSupport.buildAuthRegistrations(
            DEFAULT_AUTH_RATE_LIMIT_PROPERTIES, authMetrics);

    assertEquals(5, registrations.size());
    assertEquals(
        List.of(
            Map.entry(AUTH_REGISTER_URL, TEST_AUTH_RATE_LIMIT_REGISTER_NAMESPACE),
            Map.entry(AUTH_LOGIN_URL, TEST_AUTH_RATE_LIMIT_LOGIN_NAMESPACE),
            Map.entry(AUTH_FORGOT_PASSWORD_URL, TEST_AUTH_RATE_LIMIT_FORGOT_PASSWORD_NAMESPACE),
            Map.entry(
                AUTH_RESEND_VERIFICATION_URL, TEST_AUTH_RATE_LIMIT_RESEND_VERIFICATION_NAMESPACE),
            Map.entry(
                AUTH_VERIFY_LOGIN_DEVICE_URL, TEST_AUTH_RATE_LIMIT_VERIFY_LOGIN_DEVICE_NAMESPACE)),
        registrations.stream()
            .map(registration -> Map.entry(registration.pathPattern(), registration.namespace()))
            .toList());
  }

  @Test
  void authInterceptorsFailClosedWhenStoreIsUnavailable() {
    AuthRateLimitRegistrationDto registration =
        authRateLimitSupport
            .buildAuthRegistrations(DEFAULT_AUTH_RATE_LIMIT_PROPERTIES, authMetrics)
            .getFirst();
    RateLimitInterceptor interceptor = registration.interceptor();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Object handler = new Object();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    assertEquals(503, exception.getStatusCode().value());
  }

  @Test
  void refreshInterceptorPropagatesStoreFailure() {
    RateLimitInterceptor refreshInterceptor =
        authRateLimitSupport.buildRefreshInterceptor(
            new AuthRateLimitPropertiesDto.RateLimitDto(2, 60), authMetrics);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Object handler = new Object();

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> refreshInterceptor.preHandle(request, response, handler));

    assertEquals(TEST_RATE_LIMIT_STORE_UNAVAILABLE, exception.getMessage());
  }

  @Test
  void refreshNamespaceReturnsExpectedNamespace() {
    assertEquals(TEST_AUTH_RATE_LIMIT_REFRESH_NAMESPACE, authRateLimitSupport.refreshNamespace());
  }
}
