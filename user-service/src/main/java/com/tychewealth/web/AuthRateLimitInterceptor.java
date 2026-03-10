package com.tychewealth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthRateLimitInterceptor implements HandlerInterceptor {

  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final int maxRequests;
  private final long windowMillis;
  private final String rejectionMessage;
  private final Consumer<String> requestMetricRecorder;
  private final Consumer<String> rateLimitedMetricRecorder;
  private final Clock clock;
  private final Map<String, Deque<Long>> requestsByClient = new ConcurrentHashMap<>();

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
        Clock.systemUTC());
  }

  AuthRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder,
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
    Deque<Long> timestamps =
        requestsByClient.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());

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
    requestsByClient.clear();
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
}
