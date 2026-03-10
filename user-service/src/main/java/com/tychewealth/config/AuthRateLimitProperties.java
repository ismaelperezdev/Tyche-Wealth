package com.tychewealth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthRateLimitProperties {

  private RateLimit loginRateLimit = new RateLimit(10, 60);
  private RateLimit registerRateLimit = new RateLimit(5, 300);
  private RateLimit refreshRateLimit = new RateLimit(10, 60);

  @Getter
  @Setter
  public static class RateLimit {

    private int maxRequests;
    private long windowSeconds;

    public RateLimit() {}

    public RateLimit(int maxRequests, long windowSeconds) {
      this.maxRequests = maxRequests;
      this.windowSeconds = windowSeconds;
    }
  }
}
