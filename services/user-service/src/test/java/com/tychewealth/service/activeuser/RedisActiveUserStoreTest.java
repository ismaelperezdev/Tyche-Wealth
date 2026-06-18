package com.tychewealth.service.activeuser;

import static com.tychewealth.constants.RedisConstants.ACTIVE_USERS_KEY;
import static com.tychewealth.constants.RedisConstants.ACTIVE_USERS_LAST_REFRESH_KEY;
import static com.tychewealth.constants.RedisConstants.ACTIVE_USERS_TEMP_KEY_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisActiveUserStoreTest {

  private static final Long TEST_SECOND_USER_ID = 7L;

  @Mock private RedisTemplate<String, String> redisTemplate;

  private RedisActiveUserStore redisActiveUserStore;

  @BeforeEach
  void setUp() {
    redisActiveUserStore = new RedisActiveUserStore(redisTemplate);
  }

  @Test
  void replaceAllDeletesActiveUsersKeyWhenSetIsEmpty() {
    redisActiveUserStore.replaceAll(Set.of());

    verify(redisTemplate).delete(ACTIVE_USERS_KEY);
    verify(redisTemplate, never()).rename(any(), any());
  }

  @Test
  void replaceAllStoresUsersInTempSetAndRenamesItToActiveUsersKey() {
    SetOperations<String, String> setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    ArgumentCaptor<String> tempKeyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String[]> membersCaptor = ArgumentCaptor.forClass(String[].class);

    redisActiveUserStore.replaceAll(Set.of(TEST_USER_ID, TEST_SECOND_USER_ID));

    verify(setOperations).add(tempKeyCaptor.capture(), membersCaptor.capture());
    String tempKey = tempKeyCaptor.getValue();
    assertTrue(tempKey.startsWith(ACTIVE_USERS_TEMP_KEY_PREFIX));
    assertEquals(
        Set.of(String.valueOf(TEST_USER_ID), String.valueOf(TEST_SECOND_USER_ID)),
        Set.copyOf(Arrays.asList(membersCaptor.getValue())));
    verify(redisTemplate).rename(tempKey, ACTIVE_USERS_KEY);
  }

  @Test
  void findAllReturnsEmptySetWhenRedisHasNoActiveUsers() {
    SetOperations<String, String> setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.members(ACTIVE_USERS_KEY)).thenReturn(Set.of());

    Set<Long> result = redisActiveUserStore.findAll();

    assertTrue(result.isEmpty());
  }

  @Test
  void findAllReturnsParsedActiveUserIds() {
    SetOperations<String, String> setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.members(ACTIVE_USERS_KEY))
        .thenReturn(Set.of(String.valueOf(TEST_USER_ID), String.valueOf(TEST_SECOND_USER_ID)));

    Set<Long> result = redisActiveUserStore.findAll();

    assertEquals(Set.of(TEST_USER_ID, TEST_SECOND_USER_ID), result);
  }

  @Test
  void updateLastRefreshDeletesRefreshKeyWhenTimestampIsNull() {
    redisActiveUserStore.updateLastRefresh(null);

    verify(redisTemplate).delete(ACTIVE_USERS_LAST_REFRESH_KEY);
  }

  @Test
  void updateLastRefreshStoresIsoTimestamp() {
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    Instant refreshedAt = Instant.parse("2026-06-18T20:00:00Z");
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    redisActiveUserStore.updateLastRefresh(refreshedAt);

    verify(valueOperations).set(ACTIVE_USERS_LAST_REFRESH_KEY, refreshedAt.toString());
  }

  @Test
  void findLastRefreshReturnsEmptyWhenKeyIsMissing() {
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(ACTIVE_USERS_LAST_REFRESH_KEY)).thenReturn(null);

    Optional<Instant> result = redisActiveUserStore.findLastRefresh();

    assertTrue(result.isEmpty());
  }

  @Test
  void findLastRefreshReturnsParsedTimestamp() {
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    Instant refreshedAt = Instant.parse("2026-06-18T20:00:00Z");
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(ACTIVE_USERS_LAST_REFRESH_KEY)).thenReturn(refreshedAt.toString());

    Optional<Instant> result = redisActiveUserStore.findLastRefresh();

    assertTrue(result.isPresent());
    assertEquals(refreshedAt, result.orElseThrow());
  }

  @Test
  void findLastRefreshReturnsEmptyWhenStoredValueIsBlank() {
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(ACTIVE_USERS_LAST_REFRESH_KEY)).thenReturn(" ");

    Optional<Instant> result = redisActiveUserStore.findLastRefresh();

    assertFalse(result.isPresent());
  }
}
