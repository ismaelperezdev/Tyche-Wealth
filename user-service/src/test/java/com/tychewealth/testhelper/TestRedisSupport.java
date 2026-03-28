package com.tychewealth.testhelper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

public final class TestRedisSupport {

  private TestRedisSupport() {}

  public static StringRedisTemplate stringRedisTemplate() {
    InMemoryRedisState state = new InMemoryRedisState();
    StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
    ValueOperations<String, String> valueOperations = valueOperations(state);

    Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    Mockito.doAnswer(
            invocation -> {
              state.delete(invocation.getArgument(0));
              return true;
            })
        .when(redisTemplate)
        .delete(Mockito.anyString());

    return redisTemplate;
  }

  public static RedisTemplate<String, String> redisTemplate() {
    return redisTemplate(new InMemoryRedisState());
  }

  public static RedisTemplate<String, String> redisTemplate(InMemoryRedisState state) {
    RedisTemplate<String, String> redisTemplate = Mockito.mock(RedisTemplate.class);
    ValueOperations<String, String> valueOperations = valueOperations(state);

    Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    Mockito.when(redisTemplate.hasKey(Mockito.anyString()))
        .thenAnswer(invocation -> state.hasKey(invocation.getArgument(0)));
    Mockito.when(redisTemplate.keys(Mockito.anyString()))
        .thenAnswer(invocation -> state.keys(invocation.getArgument(0)));
    Mockito.doAnswer(
            invocation -> {
              state.delete(invocation.getArgument(0));
              return true;
            })
        .when(redisTemplate)
        .delete(Mockito.anyString());
    Mockito.doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Collection<String> keys = invocation.getArgument(0);
              state.deleteAll(keys);
              return (long) keys.size();
            })
        .when(redisTemplate)
        .delete(Mockito.anyCollection());
    Mockito.when(
            redisTemplate.execute(
                Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), Mockito.anyString()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              java.util.List<String> keys = invocation.getArgument(1);
              long ttlMillis = Long.parseLong(invocation.getArgument(2));
              return state.increment(keys.getFirst(), ttlMillis);
            });

    return redisTemplate;
  }

  private static ValueOperations<String, String> valueOperations(InMemoryRedisState state) {
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);

    Mockito.doAnswer(
            invocation -> {
              state.set(
                  invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
              return null;
            })
        .when(valueOperations)
        .set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class));
    Mockito.doAnswer(
            invocation -> {
              state.set(invocation.getArgument(0), invocation.getArgument(1), null);
              return null;
            })
        .when(valueOperations)
        .set(Mockito.anyString(), Mockito.anyString());
    Mockito.when(valueOperations.get(Mockito.anyString()))
        .thenAnswer(invocation -> state.get(invocation.getArgument(0)));

    return valueOperations;
  }

  public static final class InMemoryRedisState {
    private final Map<String, StoredValue> entries = new ConcurrentHashMap<>();

    public void set(String key, String value, Duration ttl) {
      entries.put(key, new StoredValue(value, expiresAt(ttl)));
    }

    public String get(String key) {
      purgeIfExpired(key);
      StoredValue storedValue = entries.get(key);
      return storedValue == null ? null : storedValue.value();
    }

    public boolean hasKey(String key) {
      purgeIfExpired(key);
      return entries.containsKey(key);
    }

    public void delete(String key) {
      entries.remove(key);
    }

    public void deleteAll(Collection<String> keys) {
      for (String key : keys) {
        delete(key);
      }
    }

    public Set<String> keys(String pattern) {
      purgeExpiredEntries();
      if (!pattern.endsWith("*")) {
        return entries.containsKey(pattern) ? Set.of(pattern) : Set.of();
      }

      String prefix = pattern.substring(0, pattern.length() - 1);
      Set<String> matches = ConcurrentHashMap.newKeySet();
      for (String key : entries.keySet()) {
        if (key.startsWith(prefix)) {
          matches.add(key);
        }
      }
      return matches;
    }

    public long increment(String key, long ttlMillis) {
      purgeIfExpired(key);
      long now = System.currentTimeMillis();
      StoredValue current = entries.get(key);
      long nextValue = current == null ? 1L : Long.parseLong(current.value()) + 1L;
      long expiresAt = current == null ? now + ttlMillis : current.expiresAtMillis();
      entries.put(key, new StoredValue(String.valueOf(nextValue), expiresAt));
      return nextValue;
    }

    private Long expiresAt(Duration ttl) {
      if (ttl == null) {
        return null;
      }
      return System.currentTimeMillis() + ttl.toMillis();
    }

    private void purgeExpiredEntries() {
      for (String key : new ArrayList<>(entries.keySet())) {
        purgeIfExpired(key);
      }
    }

    private void purgeIfExpired(String key) {
      StoredValue storedValue = entries.get(key);
      if (storedValue != null
          && storedValue.expiresAtMillis() != null
          && storedValue.expiresAtMillis() <= System.currentTimeMillis()) {
        entries.remove(key);
      }
    }
  }

  private record StoredValue(String value, Long expiresAtMillis) {}
}
