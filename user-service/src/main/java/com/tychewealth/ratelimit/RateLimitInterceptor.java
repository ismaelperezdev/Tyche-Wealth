package com.tychewealth.ratelimit;

import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_ACTION;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.ratelimit.support.RateLimitInterceptorConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final String namespace;
  private final String rejectionMessage;
  private final RateLimitInterceptorConfig config;
  private final boolean failClosedWhenStoreUnavailable;
  private final RateLimitStore rateLimitStore;

  public RateLimitInterceptor(RateLimitInterceptorConfig config, RateLimitStore rateLimitStore) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    String namespace = config.getNamespace();
    int maxRequests = config.getMaxRequests();
    long windowSeconds = config.getWindowSeconds();
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
    this.rejectionMessage = config.getRejectionMessage();
    this.config = config;
    this.failClosedWhenStoreUnavailable = config.isFailClosedWhenStoreUnavailable();
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    if (config.getCallbacks().getRequestMetricRecorder() != null) {
      config.getCallbacks().getRequestMetricRecorder().accept(request.getRequestURI());
    }

    long requestCount;
    try {
      requestCount =
          rateLimitStore.increment(
              namespace, resolveClientKey(request), Duration.ofMillis(windowMillis));
    } catch (RuntimeException ex) {
      if (!failClosedWhenStoreUnavailable) {
        throw ex;
      }
      if (config.getCallbacks().getRateLimitStoreFailureRecorder() != null) {
        config.getCallbacks().getRateLimitStoreFailureRecorder().accept(request.getRequestURI());
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
      if (config.getCallbacks().getRateLimitedMetricRecorder() != null) {
        config.getCallbacks().getRateLimitedMetricRecorder().accept(request.getRequestURI());
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
