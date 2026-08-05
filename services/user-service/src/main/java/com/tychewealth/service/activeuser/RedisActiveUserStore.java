package com.tychewealth.service.activeuser;

import static com.tychewealth.constants.RedisConstants.ACTIVE_USERS_KEY;
import static com.tychewealth.constants.RedisConstants.ACTIVE_USERS_LAST_REFRESH_KEY;
import static com.tychewealth.constants.RedisConstants.ACTIVE_USERS_TEMP_KEY_PREFIX;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link ActiveUserStore}.
 *
 * <p>Stores active user identifiers in a Redis set and tracks the last refresh time separately.
 * Full replacements use a temporary key followed by a rename so readers do not observe a partially
 * populated snapshot; temporary data is removed when the replacement fails.
 */
@Component
@RequiredArgsConstructor
public class RedisActiveUserStore implements ActiveUserStore {

  private final RedisTemplate<String, String> redisTemplate;

  @Override
  public void replaceAll(Set<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      redisTemplate.delete(ACTIVE_USERS_KEY);
      return;
    }

    String tempKey = ACTIVE_USERS_TEMP_KEY_PREFIX + UUID.randomUUID();
    try {
      redisTemplate
          .opsForSet()
          .add(tempKey, userIds.stream().map(String::valueOf).toArray(String[]::new));
      redisTemplate.rename(tempKey, ACTIVE_USERS_KEY);
    } catch (RuntimeException ex) {
      redisTemplate.delete(tempKey);
      throw ex;
    }
  }

  @Override
  public Set<Long> findAll() {
    Set<String> members = redisTemplate.opsForSet().members(ACTIVE_USERS_KEY);
    if (members == null || members.isEmpty()) {
      return Set.of();
    }

    Set<Long> userIds = LinkedHashSet.newLinkedHashSet(members.size());
    for (String member : members) {
      userIds.add(Long.valueOf(member));
    }
    return Set.copyOf(userIds);
  }

  @Override
  public void updateLastRefresh(Instant refreshedAt) {
    if (refreshedAt == null) {
      redisTemplate.delete(ACTIVE_USERS_LAST_REFRESH_KEY);
      return;
    }

    redisTemplate.opsForValue().set(ACTIVE_USERS_LAST_REFRESH_KEY, refreshedAt.toString());
  }

  @Override
  public Optional<Instant> findLastRefresh() {
    String value = redisTemplate.opsForValue().get(ACTIVE_USERS_LAST_REFRESH_KEY);
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    return Optional.of(Instant.parse(value));
  }
}
