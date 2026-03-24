package com.tychewealth.service.ratelimit;

import java.time.Duration;

public interface RateLimitStore {

  long increment(String namespace, String clientKey, Duration window);

  void clear(String namespace, String clientKey);

  void resetNamespace(String namespace);
}
