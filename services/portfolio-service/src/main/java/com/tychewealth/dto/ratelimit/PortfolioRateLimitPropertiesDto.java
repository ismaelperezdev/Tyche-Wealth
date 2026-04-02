package com.tychewealth.dto.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.portfolio")
public record PortfolioRateLimitPropertiesDto(RateLimitDto createRateLimit) {

  public PortfolioRateLimitPropertiesDto {
    createRateLimit =
        createRateLimit == null ? new RateLimitDto(20, 60) : createRateLimit.normalized();
  }

  public record RateLimitDto(int maxRequests, int windowSeconds) {
    public RateLimitDto normalized() {
      return new RateLimitDto(
          maxRequests <= 0 ? 20 : maxRequests, windowSeconds <= 0 ? 60 : windowSeconds);
    }
  }
}
