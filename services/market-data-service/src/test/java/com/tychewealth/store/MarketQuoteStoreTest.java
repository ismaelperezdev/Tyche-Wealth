package com.tychewealth.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.model.MarketQuote;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.types.Expiration;

@ExtendWith(MockitoExtension.class)
class MarketQuoteStoreTest {

  private static final String SYMBOL = "AAPL";
  private static final String REDIS_KEY = "market:quote:AAPL";
  private static final Duration QUOTE_TTL = Duration.ofMinutes(10);

  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private RedisConnection redisConnection;
  @Mock private RedisStringCommands redisStringCommands;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private MarketQuoteStore marketQuoteStore;

  @BeforeEach
  void setUp() {
    marketQuoteStore = new MarketQuoteStore(redisTemplate, objectMapper);
  }

  @Test
  void shouldSaveQuoteAsJsonWithTtl() throws Exception {
    MarketQuote quote = quote();
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    marketQuoteStore.save(quote);

    verify(valueOperations)
        .set(eq(REDIS_KEY), eq(objectMapper.writeValueAsString(quote)), eq(QUOTE_TTL));
  }

  @Test
  void shouldReadQuoteFromJson() throws Exception {
    MarketQuote quote = quote();
    String serializedQuote = objectMapper.writeValueAsString(quote);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(REDIS_KEY)).thenReturn(serializedQuote);

    Optional<MarketQuote> result = marketQuoteStore.findBySymbol(SYMBOL);

    assertThat(result).contains(quote);
  }

  @Test
  void shouldReturnEmptyWhenQuoteDoesNotExist() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(REDIS_KEY)).thenReturn(null);

    assertThat(marketQuoteStore.findBySymbol(SYMBOL)).isEmpty();
  }

  @Test
  void shouldSaveAllQuotesThroughPipelineWithIndividualTtl() throws Exception {
    MarketQuote firstQuote = quote();
    MarketQuote secondQuote =
        new MarketQuote(
            "MSFT", new BigDecimal("234.56000000"), LocalDateTime.of(2026, 8, 27, 12, 5));
    when(redisTemplate.executePipelined(any(RedisCallback.class)))
        .thenAnswer(
            invocation -> {
              RedisCallback<?> callback = invocation.getArgument(0);
              callback.doInRedis(redisConnection);
              return List.of();
            });
    when(redisConnection.stringCommands()).thenReturn(redisStringCommands);

    marketQuoteStore.saveAll(List.of(firstQuote, secondQuote));

    verify(redisStringCommands)
        .set(
            bytes("market:quote:AAPL"),
            bytes(objectMapper.writeValueAsString(firstQuote)),
            eq(Expiration.from(QUOTE_TTL)),
            eq(RedisStringCommands.SetOption.upsert()));
    verify(redisStringCommands)
        .set(
            bytes("market:quote:MSFT"),
            bytes(objectMapper.writeValueAsString(secondQuote)),
            eq(Expiration.from(QUOTE_TTL)),
            eq(RedisStringCommands.SetOption.upsert()));
  }

  @Test
  void shouldNotOpenPipelineForEmptyCollection() {
    marketQuoteStore.saveAll(List.of());

    verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
  }

  @Test
  void shouldKeepLastQuoteWhenSymbolsAreDuplicated() throws Exception {
    MarketQuote firstQuote = quote();
    MarketQuote lastQuote =
        new MarketQuote(
            SYMBOL, new BigDecimal("200.00000000"), LocalDateTime.of(2026, 8, 27, 12, 5));
    when(redisTemplate.executePipelined(any(RedisCallback.class)))
        .thenAnswer(
            invocation -> {
              RedisCallback<?> callback = invocation.getArgument(0);
              callback.doInRedis(redisConnection);
              return List.of();
            });
    when(redisConnection.stringCommands()).thenReturn(redisStringCommands);

    marketQuoteStore.saveAll(List.of(firstQuote, lastQuote));

    verify(redisStringCommands)
        .set(
            bytes(REDIS_KEY),
            bytes(objectMapper.writeValueAsString(lastQuote)),
            eq(Expiration.from(QUOTE_TTL)),
            eq(RedisStringCommands.SetOption.upsert()));
  }

  @Test
  void shouldFailBeforeOpeningPipelineWhenSerializationFails() throws Exception {
    ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
    when(failingObjectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("serialization failed") {});
    MarketQuoteStore store = new MarketQuoteStore(redisTemplate, failingObjectMapper);

    assertThatThrownBy(() -> store.saveAll(List.of(quote())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to serialize market quote");

    verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
  }

  @Test
  void shouldPropagateRedisPipelineFailure() {
    DataAccessResourceFailureException expectedException =
        new DataAccessResourceFailureException("Redis unavailable");
    when(redisTemplate.executePipelined(any(RedisCallback.class))).thenThrow(expectedException);

    assertThatThrownBy(() -> marketQuoteStore.saveAll(List.of(quote())))
        .isSameAs(expectedException);
  }

  @Test
  void shouldFailWhenStoredJsonIsInvalid() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(REDIS_KEY)).thenReturn("not-json");

    assertThatThrownBy(() -> marketQuoteStore.findBySymbol(SYMBOL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to deserialize market quote");
  }

  private byte[] bytes(String value) {
    return argThat(
        candidate ->
            Arrays.equals(candidate, value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private MarketQuote quote() {
    return new MarketQuote(
        SYMBOL, new BigDecimal("123.45000000"), LocalDateTime.of(2026, 8, 27, 12, 0));
  }
}
