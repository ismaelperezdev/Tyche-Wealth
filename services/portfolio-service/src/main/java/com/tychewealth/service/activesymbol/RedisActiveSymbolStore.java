package com.tychewealth.service.activesymbol;

import static com.tychewealth.constants.RedisConstants.ACTIVE_SYMBOLS_KEY;
import static com.tychewealth.constants.RedisConstants.ACTIVE_SYMBOLS_TEMP_KEY_PREFIX;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisActiveSymbolStore implements ActiveSymbolStore {

  private static final Duration TEMP_KEY_TTL = Duration.ofMinutes(2);

  private final RedisTemplate<String, String> redisTemplate;

  @Override
  public void replaceAll(Set<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      redisTemplate.delete(ACTIVE_SYMBOLS_KEY);
      return;
    }

    String tempKey = ACTIVE_SYMBOLS_TEMP_KEY_PREFIX + UUID.randomUUID();
    try {
      redisTemplate.opsForSet().add(tempKey, symbols.toArray(String[]::new));
      redisTemplate.expire(tempKey, TEMP_KEY_TTL);
      redisTemplate.rename(tempKey, ACTIVE_SYMBOLS_KEY);
    } catch (RuntimeException ex) {
      redisTemplate.delete(tempKey);
      throw ex;
    }
  }

  @Override
  public Set<String> findAll() {
    Set<String> members = redisTemplate.opsForSet().members(ACTIVE_SYMBOLS_KEY);
    if (members == null || members.isEmpty()) {
      return Set.of();
    }

    return Set.copyOf(new LinkedHashSet<>(members));
  }
}
