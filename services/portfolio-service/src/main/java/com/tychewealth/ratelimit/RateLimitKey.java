package com.tychewealth.ratelimit;

public enum RateLimitKey {
  PORTFOLIO_CREATE("rate-limit:portfolio:create", 20, 60),
  PORTFOLIO_UPDATE("rate-limit:portfolio:update", 20, 60),
  PORTFOLIO_LIST("rate-limit:portfolio:list", 120, 60),
  PORTFOLIO_RETRIEVE("rate-limit:portfolio:retrieve", 120, 60),
  PORTFOLIO_DELETE("rate-limit:portfolio:delete", 120, 60),
  ASSET_IMPORT("rate-limit:asset:import", 4, 60),
  ASSET_IMPORT_RETRIEVE("rate-limit:asset:import:retrieve", 120, 60);

  private final String namespace;
  private final int defaultMaxRequests;
  private final int defaultWindowSeconds;

  RateLimitKey(String namespace, int defaultMaxRequests, int defaultWindowSeconds) {
    this.namespace = namespace;
    this.defaultMaxRequests = defaultMaxRequests;
    this.defaultWindowSeconds = defaultWindowSeconds;
  }

  public String namespace() {
    return namespace;
  }

  public int defaultMaxRequests() {
    return defaultMaxRequests;
  }

  public int defaultWindowSeconds() {
    return defaultWindowSeconds;
  }
}
