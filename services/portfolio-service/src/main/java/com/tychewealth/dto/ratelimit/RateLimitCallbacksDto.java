package com.tychewealth.dto.ratelimit;

import java.util.Objects;
import java.util.function.Consumer;

public record RateLimitCallbacksDto(
    Consumer<String> requestMetricRecorder,
    Consumer<String> rateLimitedMetricRecorder,
    Consumer<String> rateLimitStoreFailureRecorder) {

  private static final Consumer<String> NO_OP = ignored -> {};

  public RateLimitCallbacksDto {
    requestMetricRecorder = Objects.requireNonNullElse(requestMetricRecorder, NO_OP);
    rateLimitedMetricRecorder = Objects.requireNonNullElse(rateLimitedMetricRecorder, NO_OP);
    rateLimitStoreFailureRecorder =
        Objects.requireNonNullElse(rateLimitStoreFailureRecorder, NO_OP);
  }

  public static RateLimitCallbacksDto none() {
    return new RateLimitCallbacksDto(null, null, null);
  }
}
