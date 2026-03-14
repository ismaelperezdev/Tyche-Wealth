package com.tychewealth.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.benmanes.caffeine.cache.Cache;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.testhelper.RateLimitWebTestHelper;
import com.tychewealth.testhelper.RateLimitWebTestHelper.MutableClock;
import com.tychewealth.testhelper.RateLimitWebTestHelper.MutableTicker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class RefreshRateLimitInterceptorTest {

  @Test
  void preHandleUsesRemoteAddressInsteadOfForwardedForHeader() {
    AuthMetrics authMetrics = new AuthMetrics(new SimpleMeterRegistry());
    RefreshRateLimitInterceptor interceptor = new RefreshRateLimitInterceptor(1, 60, authMetrics);

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
  void preHandleAllowsRequestsAgainAfterIdleBucketExpires() {
    AuthMetrics authMetrics = new AuthMetrics(new SimpleMeterRegistry());
    MutableTicker ticker = new MutableTicker();
    MutableClock clock = new MutableClock();
    Cache<String, Deque<Long>> cache = RateLimitWebTestHelper.buildCache(1, ticker);
    RefreshRateLimitInterceptor interceptor =
        new RefreshRateLimitInterceptor(1, 1, authMetrics, cache, clock);

    MockHttpServletRequest request = RateLimitWebTestHelper.buildRequest(null);
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
    assertThrows(
        ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    ticker.advance(Duration.ofSeconds(2));
    clock.advance(Duration.ofSeconds(2));

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
  }
}
