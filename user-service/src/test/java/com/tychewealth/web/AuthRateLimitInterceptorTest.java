package com.tychewealth.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import com.tychewealth.testhelper.RateLimitWebTestHelper;
import com.tychewealth.testhelper.RateLimitWebTestHelper.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class AuthRateLimitInterceptorTest {
  private static final String NAMESPACE = "test:auth";

  @Test
  void preHandleUsesRemoteAddressInsteadOfForwardedForHeader() {
    InMemoryRateLimitStore store = new InMemoryRateLimitStore(new MutableClock());
    AuthRateLimitInterceptor interceptor =
        new AuthRateLimitInterceptor(
            NAMESPACE, 1, 60, ErrorDefinition.RATE_LIMITED.getDescription(), null, null, store);

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
    MutableClock clock = new MutableClock();
    InMemoryRateLimitStore store = RateLimitWebTestHelper.buildStore(clock);
    AuthRateLimitInterceptor interceptor =
        new AuthRateLimitInterceptor(
            NAMESPACE, 1, 1, ErrorDefinition.RATE_LIMITED.getDescription(), null, null, store);

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
