package com.tychewealth.ratelimit;

import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_ACTION;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.ratelimit.support.RateLimitInterceptorConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

  private final RateLimitInterceptorConfig config;
  private final RateLimitStore rateLimitStore;

  public RateLimitInterceptor(RateLimitInterceptorConfig config, RateLimitStore rateLimitStore) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    if (rateLimitStore == null) {
      throw new IllegalArgumentException("rateLimitStore must not be null");
    }
    this.config = config;
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    config.getCallbacks().requestMetricRecorder().accept(request.getRequestURI());

    long requestCount;
    try {
      requestCount =
          rateLimitStore.increment(
              config.getNamespace(),
              config.getClientKeyResolver().apply(request),
              config.getWindow());
    } catch (RuntimeException ex) {
      if (!config.isFailClosedWhenStoreUnavailable()) {
        throw ex;
      }
      config.getCallbacks().rateLimitStoreFailureRecorder().accept(request.getRequestURI());
      log.error(
          REQUEST_CONFLICT + RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT,
          AUTH,
          RATE_LIMIT_ACTION,
          RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE,
          request.getRequestURI(),
          config.getNamespace(),
          config.getRejectionMessage(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Rate limit service unavailable");
    }

    if (requestCount > config.getMaxRequests()) {
      config.getCallbacks().rateLimitedMetricRecorder().accept(request.getRequestURI());
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, config.getRejectionMessage());
    }

    return true;
  }
}
