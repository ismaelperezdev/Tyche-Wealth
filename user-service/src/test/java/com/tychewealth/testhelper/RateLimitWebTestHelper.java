package com.tychewealth.testhelper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Deque;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public final class RateLimitWebTestHelper {

  private RateLimitWebTestHelper() {}

  public static MockHttpServletRequest buildRequest(String forwardedFor) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    if (forwardedFor != null) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }
    return request;
  }

  public static MockHttpServletResponse buildResponse() {
    return new MockHttpServletResponse();
  }

  public static Cache<String, Deque<Long>> buildCache(
      long expireAfterAccessSeconds, Ticker ticker) {
    return Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(expireAfterAccessSeconds))
        .ticker(ticker)
        .build();
  }

  public static final class MutableTicker implements Ticker {
    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    public void advance(Duration duration) {
      nanos += duration.toNanos();
    }
  }

  public static final class MutableClock extends Clock {
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

    public void advance(Duration duration) {
      instant = instant.plus(duration);
    }
  }
}
