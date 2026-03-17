package com.tychewealth.service.ratelimit;

import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimitStore implements RateLimitStore {

  private final RedisTemplate<String, String> redisTemplate;

  public RedisRateLimitStore(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public long increment(String namespace, String clientKey, Duration window) {
    String redisKey = buildKey(namespace, clientKey);
    ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
    Long count = valueOperations.increment(redisKey);

    if (count == null) {
      throw new IllegalStateException("Failed to increment Redis rate-limit counter");
    }

    if (count == 1L) {
      redisTemplate.expire(redisKey, window);
    }

    return count;
  }

  @Override
  public void resetNamespace(String namespace) {
    Set<String> keys = redisTemplate.keys(namespace + ":*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  private String buildKey(String namespace, String clientKey) {
    return namespace + ":" + clientKey;
  }
}
