package com.tychewealth.ratelimit;

import static com.tychewealth.constants.TestConstants.TEST_RATE_LIMIT_STORE_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.ratelimit.AuthRateLimitCallbacksDto;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.support.RateLimitInterceptorConfig;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import com.tychewealth.testhelper.RateLimitWebTestHelper;
import com.tychewealth.testhelper.RateLimitWebTestHelper.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
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
                Duration.ofSeconds(60),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                AuthRateLimitCallbacksDto.none(),
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
                Duration.ofSeconds(1),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                AuthRateLimitCallbacksDto.none(),
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
                Duration.ofSeconds(60),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                new AuthRateLimitCallbacksDto(
                    ignored -> authMetrics.incrementMetric(AuthMetricEnum.REFRESH_REQUESTS),
                    ignored -> authMetrics.incrementMetric(AuthMetricEnum.REFRESH_RATE_LIMITED),
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
                Duration.ofSeconds(1),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                new AuthRateLimitCallbacksDto(
                    ignored -> authMetrics.incrementMetric(AuthMetricEnum.REFRESH_REQUESTS),
                    ignored -> authMetrics.incrementMetric(AuthMetricEnum.REFRESH_RATE_LIMITED),
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

  @Test
  void requestAndRateLimitedCallbacksAreRecorded() {
    AtomicInteger requestCallbackCount = new AtomicInteger();
    AtomicInteger rateLimitedCallbackCount = new AtomicInteger();
    InMemoryRateLimitStore store = new InMemoryRateLimitStore(new MutableClock());
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                AUTH_NAMESPACE,
                1,
                Duration.ofSeconds(60),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                new AuthRateLimitCallbacksDto(
                    ignored -> requestCallbackCount.incrementAndGet(),
                    ignored -> rateLimitedCallbackCount.incrementAndGet(),
                    null),
                true),
            store);

    MockHttpServletRequest request = RateLimitWebTestHelper.buildRequest(null);
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handler));
    assertThrows(
        ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    assertEquals(2, requestCallbackCount.get());
    assertEquals(1, rateLimitedCallbackCount.get());
  }

  @Test
  void failClosedModeReturnsServiceUnavailableWhenStoreFails() {
    AtomicInteger storeFailureCallbackCount = new AtomicInteger();
    RateLimitStore failingStore = mock(RateLimitStore.class);
    when(failingStore.increment(anyString(), anyString(), any(Duration.class)))
        .thenThrow(new IllegalStateException(TEST_RATE_LIMIT_STORE_UNAVAILABLE));
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                AUTH_NAMESPACE,
                1,
                Duration.ofSeconds(60),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                new AuthRateLimitCallbacksDto(
                    null, null, ignored -> storeFailureCallbackCount.incrementAndGet()),
                true),
            failingStore);

    MockHttpServletRequest request = RateLimitWebTestHelper.buildRequest(null);
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> interceptor.preHandle(request, response, handler));

    assertEquals(503, exception.getStatusCode().value());
    assertEquals(1, storeFailureCallbackCount.get());
  }

  @Test
  void failOpenModePropagatesStoreFailure() {
    RateLimitStore failingStore = mock(RateLimitStore.class);
    when(failingStore.increment(anyString(), anyString(), any(Duration.class)))
        .thenThrow(new IllegalStateException(TEST_RATE_LIMIT_STORE_UNAVAILABLE));
    RateLimitInterceptor interceptor =
        new RateLimitInterceptor(
            new RateLimitInterceptorConfig(
                REFRESH_NAMESPACE,
                1,
                Duration.ofSeconds(60),
                ErrorDefinition.RATE_LIMITED.getDescription(),
                AuthRateLimitCallbacksDto.none(),
                false),
            failingStore);

    MockHttpServletRequest request = RateLimitWebTestHelper.buildRequest(null);
    MockHttpServletResponse response = RateLimitWebTestHelper.buildResponse();
    Object handler = new Object();

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> interceptor.preHandle(request, response, handler));

    assertEquals(TEST_RATE_LIMIT_STORE_UNAVAILABLE, exception.getMessage());
  }
}
