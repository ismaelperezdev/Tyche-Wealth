package com.tychewealth.ratelimit;

import static com.tychewealth.redis.RedisScripts.INCREMENT_WITH_TTL_SCRIPT;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimitStore implements RateLimitStore {

  private final RedisTemplate<String, String> redisTemplate;

  public RedisRateLimitStore(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public long increment(String namespace, String clientKey, Duration window) {
    String redisKey = namespace + ":" + clientKey;
    return Optional.of(
            redisTemplate.execute(
                INCREMENT_WITH_TTL_SCRIPT, List.of(redisKey), String.valueOf(window.toMillis())))
        .orElseThrow(
            () -> new IllegalStateException("Failed to increment Redis rate-limit counter"));
  }

  @Override
  public void clear(String namespace, String clientKey) {
    redisTemplate.delete(namespace + ":" + clientKey);
  }

  @Override
  public void resetNamespace(String namespace) {
    Set<String> keys = redisTemplate.keys(namespace + ":*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }
}
