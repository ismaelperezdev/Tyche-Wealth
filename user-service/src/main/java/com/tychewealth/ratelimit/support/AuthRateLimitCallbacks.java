package com.tychewealth.ratelimit.support;

import com.tychewealth.monitoring.AuthMetrics;
import java.util.function.Consumer;

public class AuthRateLimitCallbacks {

  private final Consumer<String> requestMetricRecorder;
  private final Consumer<String> rateLimitedMetricRecorder;
  private final Consumer<String> rateLimitStoreFailureRecorder;

  public AuthRateLimitCallbacks(
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder,
      Consumer<String> rateLimitStoreFailureRecorder) {
    this.requestMetricRecorder = requestMetricRecorder;
    this.rateLimitedMetricRecorder = rateLimitedMetricRecorder;
    this.rateLimitStoreFailureRecorder = rateLimitStoreFailureRecorder;
  }

  public static AuthRateLimitCallbacks none() {
    return new AuthRateLimitCallbacks(null, null, null);
  }

  public static AuthRateLimitCallbacks login(AuthMetrics authMetrics) {
    return new AuthRateLimitCallbacks(
        ignored -> authMetrics.recordLoginRequest(),
        ignored -> authMetrics.recordLoginRateLimited(),
        ignored -> authMetrics.recordLoginRateLimitStoreUnavailable());
  }

  public static AuthRateLimitCallbacks register(AuthMetrics authMetrics) {
    return new AuthRateLimitCallbacks(
        ignored -> authMetrics.recordRegisterRequest(),
        ignored -> authMetrics.recordRegisterRateLimited(),
        ignored -> authMetrics.recordRegisterRateLimitStoreUnavailable());
  }

  public Consumer<String> getRequestMetricRecorder() {
    return requestMetricRecorder;
  }

  public Consumer<String> getRateLimitedMetricRecorder() {
    return rateLimitedMetricRecorder;
  }

  public Consumer<String> getRateLimitStoreFailureRecorder() {
    return rateLimitStoreFailureRecorder;
  }
}
