package com.tychewealth.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.support.AuthRateLimitCallbacks;
import com.tychewealth.ratelimit.support.RateLimitInterceptorConfig;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import com.tychewealth.testhelper.RateLimitWebTestHelper;
import com.tychewealth.testhelper.RateLimitWebTestHelper.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class RateLimitInterceptorTest {
  private static final String AUTH_NAMESPACE = "test:auth";
  private static final String REFRESH_NAMESPACE = "test:refresh";

  @Test
  void authModeUsesRemoteAddressInsteadOfForwardedForHeader() {
    InMemoryRateLimitStore store = new InMemoryRateLimitStore(new MutableClock());
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                AUTH_NAMESPACE,
                1,
                60,
                ErrorDefinition.RATE_LIMITED.getDescription(),
                AuthRateLimitCallbacks.none(),
                true),
            store);

    MockHttpServletRequest firstRequest = RateLimitWebTestHelper.buildRequest("198.51.100.10");
    MockHttpServletRequest secondRequest = RateLimitWebTestHelper.buildRequest("203.0.113.20");
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(firstRequest, response, handler));
    assertThrows(
        ResponseStatusException.class,
        () -> interceptor.preHandle(secondRequest, response, handler));
  }

  @Test
  void authModeAllowsRequestsAgainAfterIdleBucketExpires() {
    MutableClock clock = new MutableClock();
    InMemoryRateLimitStore store = RateLimitWebTestHelper.buildStore(clock);
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                AUTH_NAMESPACE,
                1,
                1,
                ErrorDefinition.RATE_LIMITED.getDescription(),
                AuthRateLimitCallbacks.none(),
                true),
            store);

    MockHttpServletRequest request = RateLimitWebTestHelper.buildRequest(null);
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
    assertThrows(
        ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    clock.advance(Duration.ofSeconds(2));

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
  }

  @Test
  void refreshModeUsesRemoteAddressInsteadOfForwardedForHeader() {
    AuthMetrics authMetrics = new AuthMetrics(new SimpleMeterRegistry());
    InMemoryRateLimitStore store = new InMemoryRateLimitStore(new MutableClock());
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                REFRESH_NAMESPACE,
                1,
                60,
                ErrorDefinition.RATE_LIMITED.getDescription(),
                new AuthRateLimitCallbacks(
                    ignored -> authMetrics.recordRefreshRequest(),
                    ignored -> authMetrics.recordRefreshRateLimited(),
                    null),
                false),
            store);

    MockHttpServletRequest firstRequest = RateLimitWebTestHelper.buildRequest("198.51.100.10");
    MockHttpServletRequest secondRequest = RateLimitWebTestHelper.buildRequest("203.0.113.20");
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(firstRequest, response, handler));
    assertThrows(
        ResponseStatusException.class,
        () -> interceptor.preHandle(secondRequest, response, handler));
  }

  @Test
  void refreshModeAllowsRequestsAgainAfterIdleBucketExpires() {
    AuthMetrics authMetrics = new AuthMetrics(new SimpleMeterRegistry());
    MutableClock clock = new MutableClock();
    InMemoryRateLimitStore store = RateLimitWebTestHelper.buildStore(clock);
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                REFRESH_NAMESPACE,
                1,
                1,
                ErrorDefinition.RATE_LIMITED.getDescription(),
                new AuthRateLimitCallbacks(
                    ignored -> authMetrics.recordRefreshRequest(),
                    ignored -> authMetrics.recordRefreshRateLimited(),
                    null),
                false),
            store);

    MockHttpServletRequest request = RateLimitWebTestHelper.buildRequest(null);
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
    assertThrows(
        ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    clock.advance(Duration.ofSeconds(2));

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
  }
}
