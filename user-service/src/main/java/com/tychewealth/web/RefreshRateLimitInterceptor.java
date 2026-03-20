package com.tychewealth.web;

import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.ratelimit.RateLimitStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshRateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final String namespace;
  private final AuthMetrics authMetrics;
  private final RateLimitStore rateLimitStore;

  public RefreshRateLimitInterceptor(
      String namespace,
      int maxRequests,
      long windowSeconds,
      AuthMetrics authMetrics,
      RateLimitStore rateLimitStore) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Refresh rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Refresh rate limit window must be positive");
    }
    this.namespace = namespace;
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.authMetrics = authMetrics;
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    authMetrics.recordRefreshRequest();

    String clientKey = resolveClientKey(request);
    long requestCount =
        rateLimitStore.increment(namespace, clientKey, Duration.ofMillis(windowMillis));
    if (requestCount > maxRequests) {
      authMetrics.recordRefreshRateLimited();
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, ErrorDefinition.RATE_LIMITED.getDescription());
    }

    return true;
  }

  private String resolveClientKey(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  public void reset() {
    rateLimitStore.resetNamespace(namespace);
  }
}
