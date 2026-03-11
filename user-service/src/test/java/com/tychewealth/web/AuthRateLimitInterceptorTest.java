package com.tychewealth.web;

import static com.tychewealth.constants.AuthConstants.LOGIN_RATE_LIMIT_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class AuthRateLimitInterceptorTest {

  @Test
  void preHandleUsesRemoteAddressInsteadOfForwardedForHeader() {
    AuthRateLimitInterceptor interceptor =
        new AuthRateLimitInterceptor(1, 60, LOGIN_RATE_LIMIT_MESSAGE, null, null);

    MockHttpServletRequest firstRequest = buildRequest("198.51.100.10");
    MockHttpServletRequest secondRequest = buildRequest("203.0.113.20");
    MockHttpServletResponse response = new MockHttpServletResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(firstRequest, response, handler));
    assertThrows(
        ResponseStatusException.class,
        () -> interceptor.preHandle(secondRequest, response, handler));
  }

  @Test
  void preHandleAllowsRequestsAgainAfterIdleBucketExpires() {
    MutableTicker ticker = new MutableTicker();
    MutableClock clock = new MutableClock();
    Cache<String, Deque<Long>> cache =
        Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(1)).ticker(ticker).build();
    AuthRateLimitInterceptor interceptor =
        new AuthRateLimitInterceptor(1, 1, LOGIN_RATE_LIMIT_MESSAGE, null, null, cache, clock);

    MockHttpServletRequest request = buildRequest(null);
    MockHttpServletResponse response = new MockHttpServletResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
    assertThrows(
        ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    ticker.advance(Duration.ofSeconds(2));
    clock.advance(Duration.ofSeconds(2));

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
  }

  private static MockHttpServletRequest buildRequest(String forwardedFor) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    if (forwardedFor != null) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }
    return request;
  }

  private static final class MutableTicker implements Ticker {
    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    private void advance(Duration duration) {
      nanos += duration.toNanos();
    }
  }

  private static final class MutableClock extends Clock {
    private Instant instant = Instant.parse("2026-03-11T00:00:00Z");

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }
  }
}
