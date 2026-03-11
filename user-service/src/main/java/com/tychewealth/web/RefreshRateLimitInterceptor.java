package com.tychewealth.web;

import static com.tychewealth.constants.AuthConstants.REFRESH_RATE_LIMIT_MESSAGE;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tychewealth.service.monitoring.AuthMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshRateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final AuthMetrics authMetrics;
  private final Clock clock;
  private final Cache<String, Deque<Long>> requestsByClient;

  public RefreshRateLimitInterceptor(int maxRequests, long windowSeconds, AuthMetrics authMetrics) {
    this(
        maxRequests,
        windowSeconds,
        authMetrics,
        buildRequestsByClientCache(windowSeconds),
        Clock.systemUTC());
  }

  RefreshRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      AuthMetrics authMetrics,
      Cache<String, Deque<Long>> requestsByClient,
      Clock clock) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Refresh rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Refresh rate limit window must be positive");
    }
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.authMetrics = authMetrics;
    this.requestsByClient = requestsByClient;
    this.clock = clock;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    authMetrics.recordRefreshRequest();

    String clientKey = resolveClientKey(request);
    long now = clock.millis();
    Deque<Long> timestamps = requestsByClient.get(clientKey, ignored -> new ArrayDeque<>());

    synchronized (timestamps) {
      evictExpiredRequests(timestamps, now);
      if (timestamps.size() >= maxRequests) {
        authMetrics.recordRefreshRateLimited();
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, REFRESH_RATE_LIMIT_MESSAGE);
      }
      timestamps.addLast(now);
    }

    return true;
  }

  private void evictExpiredRequests(Deque<Long> timestamps, long now) {
    long threshold = now - windowMillis;
    while (!timestamps.isEmpty() && timestamps.peekFirst() < threshold) {
      timestamps.removeFirst();
    }
  }

  private String resolveClientKey(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  public void reset() {
    requestsByClient.invalidateAll();
  }

  private static Cache<String, Deque<Long>> buildRequestsByClientCache(long windowSeconds) {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(windowSeconds)).build();
  }
}
