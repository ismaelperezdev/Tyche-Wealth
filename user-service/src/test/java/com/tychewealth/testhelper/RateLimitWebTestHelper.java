package com.tychewealth.testhelper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

  public static InMemoryRateLimitStore buildStore(MutableClock clock) {
    return new InMemoryRateLimitStore(clock);
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
