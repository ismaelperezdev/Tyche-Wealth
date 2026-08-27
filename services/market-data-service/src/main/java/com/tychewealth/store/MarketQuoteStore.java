package com.tychewealth.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.model.MarketQuote;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

/** Stores the latest market quote for each symbol in Redis. */
@Component
public class MarketQuoteStore {

  private static final String KEY_PREFIX = "market:quote:";
  private static final Duration QUOTE_TTL = Duration.ofMinutes(10);

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  public MarketQuoteStore(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public void save(MarketQuote quote) {
    redisTemplate.opsForValue().set(KEY_PREFIX + quote.symbol(), serialize(quote), QUOTE_TTL);
  }

  public void saveAll(Collection<MarketQuote> quotes) {
    if (quotes.isEmpty()) {
      return;
    }

    Map<String, String> serializedQuotes = new LinkedHashMap<>();
    quotes.forEach(quote -> serializedQuotes.put(KEY_PREFIX + quote.symbol(), serialize(quote)));

    redisTemplate.executePipelined(
        (RedisCallback<Object>)
            connection -> {
              serializedQuotes.forEach((key, value) -> set(connection, key, value));
              return null;
            });
  }

  public Optional<MarketQuote> findBySymbol(String symbol) {
    String serializedQuote = redisTemplate.opsForValue().get(KEY_PREFIX + symbol);
    if (serializedQuote == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(objectMapper.readValue(serializedQuote, MarketQuote.class));
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Unable to deserialize market quote", error);
    }
  }

  private String serialize(MarketQuote quote) {
    try {
      return objectMapper.writeValueAsString(quote);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Unable to serialize market quote", error);
    }
  }

  private void set(RedisConnection connection, String key, String value) {
    connection
        .stringCommands()
        .set(
            key.getBytes(StandardCharsets.UTF_8),
            value.getBytes(StandardCharsets.UTF_8),
            Expiration.from(QUOTE_TTL),
            RedisStringCommands.SetOption.upsert());
  }
}
