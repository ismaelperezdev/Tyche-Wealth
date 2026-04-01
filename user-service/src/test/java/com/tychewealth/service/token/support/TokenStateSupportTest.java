package com.tychewealth.service.token.support;

import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_TOKEN_STATE_UNAVAILABLE;
import static com.tychewealth.constants.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.monitoring.AuthMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TokenStateSupportTest {

  private TokenStateSupport tokenStateSupport;
  private RedisTemplate<String, String> redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private SimpleMeterRegistry meterRegistry;
  private AuthMetrics authMetrics;

  @BeforeEach
  void setUp() {
    tokenStateSupport = new TokenStateSupport();
    redisTemplate = mock(RedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    meterRegistry = new SimpleMeterRegistry();
    authMetrics = new AuthMetrics(meterRegistry);
  }

  @Test
  void buildAccessTokenRevocationKeyPrefixesTokenId() {
    assertEquals(
        "auth:access-token:blacklist:jti:" + TEST_ACCESS_TOKEN_JTI,
        tokenStateSupport.buildAccessTokenRevocationKey(TEST_ACCESS_TOKEN_JTI));
  }

  @Test
  void buildRefreshTokenAccessTokenLinkKeyHashesRefreshToken() {
    String key = tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(TEST_REFRESH_TOKEN_EXISTING);

    assertTrue(key.matches("^auth:refresh-token:access-token-jti:[a-f0-9]{64}$"));
    assertFalse(key.endsWith(TEST_REFRESH_TOKEN_EXISTING));
  }

  @Test
  void findAccessTokenJtiByRefreshTokenReturnsStoredValue() {
    when(valueOperations.get(
            tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(TEST_REFRESH_TOKEN_EXISTING)))
        .thenReturn(TEST_ACCESS_TOKEN_JTI);

    Optional<String> result =
        tokenStateSupport.findAccessTokenJtiByRefreshToken(
            redisTemplate, TEST_REFRESH_TOKEN_EXISTING);

    assertEquals(Optional.of(TEST_ACCESS_TOKEN_JTI), result);
  }

  @Test
  void isAccessTokenRevokedReturnsRedisResultWhenAvailable() {
    when(redisTemplate.hasKey(
            tokenStateSupport.buildAccessTokenRevocationKey(TEST_ACCESS_TOKEN_JTI)))
        .thenReturn(true);

    assertTrue(
        tokenStateSupport.isAccessTokenRevoked(redisTemplate, authMetrics, TEST_ACCESS_TOKEN_JTI));
  }

  @Test
  void isAccessTokenRevokedFailsClosedWhenRedisThrows() {
    when(redisTemplate.hasKey(
            tokenStateSupport.buildAccessTokenRevocationKey(TEST_ACCESS_TOKEN_JTI)))
        .thenThrow(new IllegalStateException(TEST_RATE_LIMIT_STORE_UNAVAILABLE));

    boolean revoked =
        tokenStateSupport.isAccessTokenRevoked(redisTemplate, authMetrics, TEST_ACCESS_TOKEN_JTI);

    assertTrue(revoked);
    assertEquals(1.0, meterRegistry.get(METRIC_AUTH_TOKEN_STATE_UNAVAILABLE).counter().count());
  }

  @Test
  void extractBearerTokenRejectsInvalidHeaders() {
    assertThrows(
        AuthException.class, () -> tokenStateSupport.extractBearerToken(TEST_ACCESS_TOKEN));
  }

  @Test
  void extractBearerTokenReturnsTrimmedToken() {
    assertEquals(TEST_ACCESS_TOKEN, tokenStateSupport.extractBearerToken(TEST_BEARER_ACCESS_TOKEN));
  }
}
