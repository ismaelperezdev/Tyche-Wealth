package com.tychewealth.ratelimit.support;

import com.tychewealth.dto.ratelimit.AuthRateLimitCallbacksDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import lombok.Getter;

@Getter
public class RateLimitInterceptorConfig {
  private static final Function<HttpServletRequest, String> REMOTE_ADDRESS_RESOLVER =
      HttpServletRequest::getRemoteAddr;

  private final String namespace;
  private final int maxRequests;
  private final Duration window;
  private final String rejectionMessage;
  private final AuthRateLimitCallbacksDto callbacks;
  private final boolean failClosedWhenStoreUnavailable;
  private final Function<HttpServletRequest, String> clientKeyResolver;

  public RateLimitInterceptorConfig(
      String namespace,
      int maxRequests,
      Duration window,
      String rejectionMessage,
      AuthRateLimitCallbacksDto callbacks,
      boolean failClosedWhenStoreUnavailable) {
    this(
        namespace,
        maxRequests,
        window,
        rejectionMessage,
        callbacks,
        failClosedWhenStoreUnavailable,
        REMOTE_ADDRESS_RESOLVER);
  }

  public RateLimitInterceptorConfig(
      String namespace,
      int maxRequests,
      Duration window,
      String rejectionMessage,
      AuthRateLimitCallbacksDto callbacks,
      boolean failClosedWhenStoreUnavailable,
      Function<HttpServletRequest, String> clientKeyResolver) {
    if (namespace == null || namespace.isBlank()) {
      throw new IllegalArgumentException("namespace must be non-empty");
    }
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Rate limit max requests must be positive");
    }
    if (window == null || window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("Rate limit window must be positive");
    }
    this.namespace = namespace;
    this.maxRequests = maxRequests;
    this.window = window;
    this.rejectionMessage = rejectionMessage;
    this.callbacks = Objects.requireNonNullElseGet(callbacks, AuthRateLimitCallbacksDto::none);
    this.failClosedWhenStoreUnavailable = failClosedWhenStoreUnavailable;
    this.clientKeyResolver = Objects.requireNonNullElse(clientKeyResolver, REMOTE_ADDRESS_RESOLVER);
  }
}
