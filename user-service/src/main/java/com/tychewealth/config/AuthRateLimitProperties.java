package com.tychewealth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthRateLimitProperties {

  @Valid private RateLimit loginRateLimit = new RateLimit(10, 60);

  @Valid private RateLimit registerRateLimit = new RateLimit(5, 300);

  @Valid private RateLimit refreshRateLimit = new RateLimit(10, 60);

  @Getter
  @Setter
  public static class RateLimit {

    @Positive private int maxRequests;

    @Positive private long windowSeconds;

    /**
 * Creates a new RateLimit instance with default field values.
 */
public RateLimit() {}

    /**
     * Create a RateLimit with the specified maximum requests and window duration.
     *
     * @param maxRequests   maximum number of requests allowed in the window; must be greater than 0
     * @param windowSeconds duration of the window in seconds; must be greater than 0
     */
    public RateLimit(int maxRequests, long windowSeconds) {
      this.maxRequests = maxRequests;
      this.windowSeconds = windowSeconds;
    }
  }
}
