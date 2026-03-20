package com.tychewealth.service.ratelimit;

import java.time.Duration;

public interface RateLimitStore {

  long increment(String namespace, String clientKey, Duration window);

  void resetNamespace(String namespace);
}
