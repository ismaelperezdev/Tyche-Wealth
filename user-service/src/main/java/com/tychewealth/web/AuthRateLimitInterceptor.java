package com.tychewealth.web;

import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_ACTION;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.service.ratelimit.RateLimitStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class AuthRateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final String namespace;
  private final String rejectionMessage;
  private final Consumer<String> requestMetricRecorder;
  private final Consumer<String> rateLimitedMetricRecorder;
  private final Consumer<String> rateLimitStoreFailureRecorder;
  private final RateLimitStore rateLimitStore;

  public AuthRateLimitInterceptor(
      String namespace,
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder,
      Consumer<String> rateLimitStoreFailureRecorder,
      RateLimitStore rateLimitStore) {
    if (namespace == null || namespace.isBlank()) {
      throw new IllegalArgumentException("namespace must be non-empty");
    }
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Rate limit window must be positive");
    }
    if (rateLimitStore == null) {
      throw new IllegalArgumentException("rateLimitStore must not be null");
    }
    this.namespace = namespace;
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.rejectionMessage = rejectionMessage;
    this.requestMetricRecorder = requestMetricRecorder;
    this.rateLimitedMetricRecorder = rateLimitedMetricRecorder;
    this.rateLimitStoreFailureRecorder = rateLimitStoreFailureRecorder;
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (requestMetricRecorder != null) {
      requestMetricRecorder.accept(request.getRequestURI());
    }

    String clientKey = resolveClientKey(request);
    long requestCount;
    try {
      requestCount =
          rateLimitStore.increment(namespace, clientKey, Duration.ofMillis(windowMillis));
    } catch (RuntimeException ex) {
      if (rateLimitStoreFailureRecorder != null) {
        rateLimitStoreFailureRecorder.accept(request.getRequestURI());
      }
      log.error(
          REQUEST_CONFLICT + RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT,
          AUTH,
          RATE_LIMIT_ACTION,
          RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE,
          request.getRequestURI(),
          namespace,
          rejectionMessage,
          ex);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Rate limit service unavailable");
    }
    if (requestCount > maxRequests) {
      if (rateLimitedMetricRecorder != null) {
        rateLimitedMetricRecorder.accept(request.getRequestURI());
      }
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, rejectionMessage);
    }

    return true;
  }

  public void reset() {
    rateLimitStore.resetNamespace(namespace);
  }

  private String resolveClientKey(HttpServletRequest request) {
    return request.getRemoteAddr();
  }
}
