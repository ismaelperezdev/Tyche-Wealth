package com.tychewealth.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthRateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final String rejectionMessage;
  private final Consumer<String> requestMetricRecorder;
  private final Consumer<String> rateLimitedMetricRecorder;
  private final Clock clock;
  private final Cache<String, Deque<Long>> requestsByClient;

  public AuthRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder) {
    this(
        maxRequests,
        windowSeconds,
        rejectionMessage,
        requestMetricRecorder,
        rateLimitedMetricRecorder,
        buildRequestsByClientCache(windowSeconds),
        Clock.systemUTC());
  }

  AuthRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder,
      Cache<String, Deque<Long>> requestsByClient,
      Clock clock) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Rate limit window must be positive");
    }
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.rejectionMessage = rejectionMessage;
    this.requestMetricRecorder = requestMetricRecorder;
    this.rateLimitedMetricRecorder = rateLimitedMetricRecorder;
    this.requestsByClient = requestsByClient;
    this.clock = clock;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (requestMetricRecorder != null) {
      requestMetricRecorder.accept(request.getRequestURI());
    }

    String clientKey = resolveClientKey(request);
    long now = clock.millis();
    Deque<Long> timestamps = requestsByClient.get(clientKey, ignored -> new ArrayDeque<>());

    synchronized (timestamps) {
      evictExpiredRequests(timestamps, now);
      if (timestamps.size() >= maxRequests) {
        if (rateLimitedMetricRecorder != null) {
          rateLimitedMetricRecorder.accept(request.getRequestURI());
        }
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, rejectionMessage);
      }
      timestamps.addLast(now);
    }

    return true;
  }

  public void reset() {
    requestsByClient.invalidateAll();
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

  private static Cache<String, Deque<Long>> buildRequestsByClientCache(long windowSeconds) {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(windowSeconds)).build();
  }
}
