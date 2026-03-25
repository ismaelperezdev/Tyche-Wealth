package com.tychewealth.testhelper;

import com.tychewealth.service.ratelimit.RateLimitStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitStore implements RateLimitStore {

  private final Clock clock;
  private final Map<String, CounterEntry> counters = new ConcurrentHashMap<>();

  public InMemoryRateLimitStore(Clock clock) {
    this.clock = clock;
  }

  @Override
  public long increment(String namespace, String clientKey, Duration window) {
    long now = clock.millis();
    String key = buildKey(namespace, clientKey);

    CounterEntry updatedEntry =
        counters.compute(
            key,
            (ignored, existing) -> {
              if (existing == null || existing.expiresAtMillis <= now) {
                return new CounterEntry(1, now + window.toMillis());
              }
              return new CounterEntry(existing.count + 1, existing.expiresAtMillis);
            });

    return updatedEntry.count;
  }

  @Override
  public void clear(String namespace, String clientKey) {
    counters.remove(buildKey(namespace, clientKey));
  }

  @Override
  public void resetNamespace(String namespace) {
    String prefix = namespace + ":";
    counters.keySet().removeIf(key -> key.startsWith(prefix));
  }

  private String buildKey(String namespace, String clientKey) {
    return namespace + ":" + clientKey;
  }

  private record CounterEntry(long count, long expiresAtMillis) {}
}
