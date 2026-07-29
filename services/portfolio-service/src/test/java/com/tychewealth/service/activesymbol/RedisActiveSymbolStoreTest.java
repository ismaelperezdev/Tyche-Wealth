package com.tychewealth.service.activesymbol;

import static com.tychewealth.constants.RedisConstants.ACTIVE_SYMBOLS_KEY;
import static com.tychewealth.constants.RedisConstants.ACTIVE_SYMBOLS_TEMP_KEY_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

@ExtendWith(MockitoExtension.class)
class RedisActiveSymbolStoreTest {

  @Mock private RedisTemplate<String, String> redisTemplate;

  private RedisActiveSymbolStore redisActiveSymbolStore;

  @BeforeEach
  void setUp() {
    redisActiveSymbolStore = new RedisActiveSymbolStore(redisTemplate);
  }

  @Test
  void replaceAllDeletesActiveSymbolsKeyWhenSetIsEmpty() {
    redisActiveSymbolStore.replaceAll(Set.of());

    verify(redisTemplate).delete(ACTIVE_SYMBOLS_KEY);
    verify(redisTemplate, never()).rename(any(), any());
  }

  @Test
  void replaceAllStoresSymbolsInTempSetAndRenamesItToActiveSymbolsKey() {
    SetOperations<String, String> setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    ArgumentCaptor<String> tempKeyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String[]> membersCaptor = ArgumentCaptor.forClass(String[].class);

    redisActiveSymbolStore.replaceAll(Set.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT));

    verify(setOperations).add(tempKeyCaptor.capture(), membersCaptor.capture());
    String tempKey = tempKeyCaptor.getValue();
    assertTrue(tempKey.startsWith(ACTIVE_SYMBOLS_TEMP_KEY_PREFIX));
    assertEquals(
        Set.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT),
        Set.copyOf(Arrays.asList(membersCaptor.getValue())));
    verify(redisTemplate).rename(tempKey, ACTIVE_SYMBOLS_KEY);
  }

  @Test
  void findAllReturnsEmptySetWhenRedisHasNoActiveSymbols() {
    SetOperations<String, String> setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.members(ACTIVE_SYMBOLS_KEY)).thenReturn(Set.of());

    Set<String> result = redisActiveSymbolStore.findAll();

    assertTrue(result.isEmpty());
  }

  @Test
  void findAllReturnsStoredSymbols() {
    SetOperations<String, String> setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.members(ACTIVE_SYMBOLS_KEY))
        .thenReturn(Set.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT));

    Set<String> result = redisActiveSymbolStore.findAll();

    assertEquals(Set.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT), result);
  }
}
