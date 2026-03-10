package com.tychewealth.web;

import com.tychewealth.service.monitoring.AuthMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshRateLimitInterceptor implements HandlerInterceptor {

  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final int maxRequests;
  private final long windowMillis;
  private final AuthMetrics authMetrics;
  private final Clock clock;
  private final Map<String, Deque<Long>> requestsByClient = new ConcurrentHashMap<>();

  public RefreshRateLimitInterceptor(int maxRequests, long windowSeconds, AuthMetrics authMetrics) {
    this(maxRequests, windowSeconds, authMetrics, Clock.systemUTC());
  }

  RefreshRateLimitInterceptor(
      int maxRequests, long windowSeconds, AuthMetrics authMetrics, Clock clock) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Refresh rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Refresh rate limit window must be positive");
    }
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.authMetrics = authMetrics;
    this.clock = clock;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    authMetrics.recordRefreshRequest();

    String clientKey = resolveClientKey(request);
    long now = clock.millis();
    Deque<Long> timestamps =
        requestsByClient.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());

    synchronized (timestamps) {
      evictExpiredRequests(timestamps, now);
      if (timestamps.size() >= maxRequests) {
        authMetrics.recordRefreshRateLimited();
        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS, "Too many refresh token requests");
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
    String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
    if (StringUtils.hasText(forwardedFor)) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  public void reset() {
    requestsByClient.clear();
  }
}
