package com.tychewealth.dto.ratelimit;

import com.tychewealth.ratelimit.RateLimitKey;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitPropertiesDto {

  private final Map<RateLimitKey, RateLimitDto> rules = defaultRules();

  public Map<RateLimitKey, RateLimitDto> getRules() {
    return rules;
  }

  public RateLimitDto ruleFor(RateLimitKey key) {
    RateLimitDto configured = rules.get(key);
    if (configured == null) {
      return new RateLimitDto(key.defaultMaxRequests(), key.defaultWindowSeconds());
    }
    return configured.normalized(key.defaultMaxRequests(), key.defaultWindowSeconds());
  }

  private static Map<RateLimitKey, RateLimitDto> defaultRules() {
    Map<RateLimitKey, RateLimitDto> defaults = new EnumMap<>(RateLimitKey.class);
    for (RateLimitKey key : RateLimitKey.values()) {
      defaults.put(key, new RateLimitDto(key.defaultMaxRequests(), key.defaultWindowSeconds()));
    }
    return defaults;
  }

  public static class RateLimitDto {
    private int maxRequests;
    private int windowSeconds;

    public RateLimitDto() {}

    public RateLimitDto(int maxRequests, int windowSeconds) {
      this.maxRequests = maxRequests;
      this.windowSeconds = windowSeconds;
    }

    public int getMaxRequests() {
      return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
      this.maxRequests = maxRequests;
    }

    public int getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
      this.windowSeconds = windowSeconds;
    }

    public RateLimitDto normalized(int defaultMaxRequests, int defaultWindowSeconds) {
      return new RateLimitDto(
          maxRequests <= 0 ? defaultMaxRequests : maxRequests,
          windowSeconds <= 0 ? defaultWindowSeconds : windowSeconds);
    }
  }
}
