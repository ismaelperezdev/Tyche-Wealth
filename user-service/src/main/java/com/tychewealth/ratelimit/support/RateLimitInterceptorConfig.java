package com.tychewealth.ratelimit.support;

import lombok.Getter;

@Getter
public class RateLimitInterceptorConfig {

  private final String namespace;
  private final int maxRequests;
  private final long windowSeconds;
  private final String rejectionMessage;
  private final AuthRateLimitCallbacks callbacks;
  private final boolean failClosedWhenStoreUnavailable;

  public RateLimitInterceptorConfig(
      String namespace,
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      AuthRateLimitCallbacks callbacks,
      boolean failClosedWhenStoreUnavailable) {
    this.namespace = namespace;
    this.maxRequests = maxRequests;
    this.windowSeconds = windowSeconds;
    this.rejectionMessage = rejectionMessage;
    this.callbacks = callbacks;
    this.failClosedWhenStoreUnavailable = failClosedWhenStoreUnavailable;
  }
}
