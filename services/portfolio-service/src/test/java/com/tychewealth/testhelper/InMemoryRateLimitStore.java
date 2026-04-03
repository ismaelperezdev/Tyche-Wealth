package com.tychewealth.testhelper;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.dto.ratelimit.RateLimitPropertiesDto;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimitStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

public class InMemoryRateLimitStore implements RateLimitStore {

  private final Clock clock;
  private final Map<String, CounterEntry> counters = new ConcurrentHashMap<>();

  public InMemoryRateLimitStore(Clock clock) {
    this.clock = clock;
  }

  public static void assertRateLimited(
      RateLimitKey rateLimitKey, MockHttpServletRequest request, HandlerMethod handlerMethod) {
    InMemoryRateLimitStore rateLimitStore = new InMemoryRateLimitStore(Clock.systemUTC());
    RateLimitPropertiesDto properties = new RateLimitPropertiesDto();
    properties.getRules().put(rateLimitKey, new RateLimitPropertiesDto.RateLimitDto(1, 60));
    RateLimitInterceptor interceptor = new RateLimitInterceptor(properties, rateLimitStore);

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USER_ID, null));

    MockHttpServletResponse response = new MockHttpServletResponse();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response, handlerMethod));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> interceptor.preHandle(request, response, handlerMethod));

    assertEquals(429, exception.getStatusCode().value());
  }

  @Override
  public long increment(String namespace, String clientKey, Duration window) {
    long now = clock.millis();
    String key = buildKey(namespace, clientKey);

    CounterEntry updatedEntry =
        counters.compute(
            key,
            (ignored, existing) -> {
              if (existing == null || existing.expiresAtMillis <= now) {
                return new CounterEntry(1, now + window.toMillis());
              }
              return new CounterEntry(existing.count + 1, existing.expiresAtMillis);
            });

    return updatedEntry.count;
  }

  @Override
  public void clear(String namespace, String clientKey) {
    counters.remove(buildKey(namespace, clientKey));
  }

  @Override
  public void resetNamespace(String namespace) {
    String prefix = namespace + ":";
    counters.keySet().removeIf(key -> key.startsWith(prefix));
  }

  private String buildKey(String namespace, String clientKey) {
    return namespace + ":" + clientKey;
  }

  private record CounterEntry(long count, long expiresAtMillis) {}
}
