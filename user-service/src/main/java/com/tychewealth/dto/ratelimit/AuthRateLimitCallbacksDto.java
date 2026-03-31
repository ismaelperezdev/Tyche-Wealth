package com.tychewealth.dto.ratelimit;

import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.monitoring.AuthMetrics;
import java.util.Objects;
import java.util.function.Consumer;

public record AuthRateLimitCallbacksDto(
    Consumer<String> requestMetricRecorder,
    Consumer<String> rateLimitedMetricRecorder,
    Consumer<String> rateLimitStoreFailureRecorder) {
  private static final Consumer<String> NO_OP = ignored -> {};

  public AuthRateLimitCallbacksDto {
    requestMetricRecorder = Objects.requireNonNullElse(requestMetricRecorder, NO_OP);
    rateLimitedMetricRecorder = Objects.requireNonNullElse(rateLimitedMetricRecorder, NO_OP);
    rateLimitStoreFailureRecorder =
        Objects.requireNonNullElse(rateLimitStoreFailureRecorder, NO_OP);
  }

  public static AuthRateLimitCallbacksDto none() {
    return new AuthRateLimitCallbacksDto(null, null, null);
  }

  public static AuthRateLimitCallbacksDto login(AuthMetrics authMetrics) {
    return new AuthRateLimitCallbacksDto(
        ignored -> authMetrics.incrementMetric(AuthMetricEnum.LOGIN_REQUESTS),
        ignored -> authMetrics.incrementMetric(AuthMetricEnum.LOGIN_RATE_LIMITED),
        ignored -> authMetrics.incrementMetric(AuthMetricEnum.LOGIN_RATE_LIMIT_STORE_UNAVAILABLE));
  }

  public static AuthRateLimitCallbacksDto register(AuthMetrics authMetrics) {
    return new AuthRateLimitCallbacksDto(
        ignored -> authMetrics.incrementMetric(AuthMetricEnum.REGISTER_REQUESTS),
        ignored -> authMetrics.incrementMetric(AuthMetricEnum.REGISTER_RATE_LIMITED),
        ignored ->
            authMetrics.incrementMetric(AuthMetricEnum.REGISTER_RATE_LIMIT_STORE_UNAVAILABLE));
  }
}
