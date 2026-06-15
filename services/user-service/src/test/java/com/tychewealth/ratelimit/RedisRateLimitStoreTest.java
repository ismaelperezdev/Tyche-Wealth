package com.tychewealth.ratelimit;

import static com.tychewealth.redis.RedisScripts.INCREMENT_WITH_TTL_SCRIPT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisRateLimitStoreTest {

  private static final String TEST_AUTH_RATE_LIMIT_LOGIN_NAMESPACE = "rate-limit:auth:login";
  private static final String TEST_RATE_LIMIT_NAMESPACE = "namespace";
  private static final String TEST_RATE_LIMIT_CLIENT = "client";

  @Mock private RedisTemplate<String, String> redisTemplate;

  private RedisRateLimitStore rateLimitStore;

  @BeforeEach
  void setUp() {
    rateLimitStore = new RedisRateLimitStore(redisTemplate);
  }

  @Test
  void incrementExecutesScriptWithNamespacedKeyAndWindowTtl() {
    when(redisTemplate.execute(
            INCREMENT_WITH_TTL_SCRIPT,
            List.of(TEST_AUTH_RATE_LIMIT_LOGIN_NAMESPACE + ":127.0.0.1"),
            "60000"))
        .thenReturn(2L);

    long result =
        rateLimitStore.increment(
            TEST_AUTH_RATE_LIMIT_LOGIN_NAMESPACE, "127.0.0.1", Duration.ofMinutes(1));

    assertEquals(2L, result);
  }

  @Test
  void incrementFailsWhenRedisReturnsNull() {
    Duration window = Duration.ofSeconds(1);
    when(redisTemplate.execute(
            INCREMENT_WITH_TTL_SCRIPT,
            List.of(TEST_RATE_LIMIT_NAMESPACE + ":" + TEST_RATE_LIMIT_CLIENT),
            "1000"))
        .thenReturn(null);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                rateLimitStore.increment(
                    TEST_RATE_LIMIT_NAMESPACE, TEST_RATE_LIMIT_CLIENT, window));

    assertEquals("Failed to increment Redis rate-limit counter", exception.getMessage());
  }

  @Test
  void clearDeletesNamespacedKey() {
    rateLimitStore.clear(TEST_RATE_LIMIT_NAMESPACE, TEST_RATE_LIMIT_CLIENT);

    verify(redisTemplate).delete(TEST_RATE_LIMIT_NAMESPACE + ":" + TEST_RATE_LIMIT_CLIENT);
  }

  @Test
  void resetNamespaceDeletesAllKeysWithinNamespace() {
    Set<String> keys =
        Set.of(TEST_RATE_LIMIT_NAMESPACE + ":client-a", TEST_RATE_LIMIT_NAMESPACE + ":client-b");
    when(redisTemplate.keys(TEST_RATE_LIMIT_NAMESPACE + ":*")).thenReturn(keys);

    rateLimitStore.resetNamespace(TEST_RATE_LIMIT_NAMESPACE);

    verify(redisTemplate).delete(keys);
  }

  @Test
  void resetNamespaceDoesNothingWhenNoKeysExist() {
    when(redisTemplate.keys(TEST_RATE_LIMIT_NAMESPACE + ":*")).thenReturn(Set.of());

    rateLimitStore.resetNamespace(TEST_RATE_LIMIT_NAMESPACE);

    verify(redisTemplate, never()).delete(anySet());
  }
}
