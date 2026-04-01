package com.tychewealth.service.token;

import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_TOKEN_STATE_UNAVAILABLE;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_BEARER_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_RATE_LIMIT_STORE_UNAVAILABLE;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.service.token.support.TokenStateSupport;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class TokenStateStoreTest {

  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private AccessTokenCodec accessTokenCodec;

  private SimpleMeterRegistry meterRegistry;
  private AuthMetrics authMetrics;
  private TokenStateSupport tokenStateSupport;
  private TokenStateStore tokenStateStore;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    authMetrics = new AuthMetrics(meterRegistry);
    tokenStateSupport = new TokenStateSupport();
    tokenStateStore =
        new TokenStateStore(redisTemplate, accessTokenCodec, authMetrics, tokenStateSupport);
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void revokeAccessTokenIfPresentDoesNothingWhenHeaderIsBlank() {
    tokenStateStore.revokeAccessTokenIfPresent(" ");

    verify(accessTokenCodec, never()).extractTokenId(any());
  }

  @Test
  void revokeAccessTokenIfPresentRevokesTokenUsingExtractedData() {
    Instant expiresAt = Instant.now().plusSeconds(60);
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(accessTokenCodec.extractTokenId(TEST_ACCESS_TOKEN)).thenReturn(TEST_ACCESS_TOKEN_JTI);
    when(accessTokenCodec.extractExpiration(TEST_ACCESS_TOKEN)).thenReturn(expiresAt);

    tokenStateStore.revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);

    verify(valueOperations)
        .set(
            eq(tokenStateSupport.buildAccessTokenRevocationKey(TEST_ACCESS_TOKEN_JTI)),
            eq("revoked"),
            any(Duration.class));
  }

  @Test
  void revokeAccessTokenDoesNothingWhenTokenIsExpired() {
    tokenStateStore.revokeAccessToken(TEST_ACCESS_TOKEN_JTI, Instant.now().minusSeconds(5));

    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  void linkRefreshTokenToAccessTokenStoresJtiWithTtl() {
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    tokenStateStore.linkRefreshTokenToAccessToken(
        TEST_REFRESH_TOKEN_EXISTING, TEST_ACCESS_TOKEN_JTI, Instant.now().plusSeconds(60));

    verify(valueOperations)
        .set(
            eq(tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(TEST_REFRESH_TOKEN_EXISTING)),
            eq(TEST_ACCESS_TOKEN_JTI),
            any(Duration.class));
  }

  @Test
  void findAccessTokenJtiByRefreshTokenDelegatesToSupport() {
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(
            tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(TEST_REFRESH_TOKEN_EXISTING)))
        .thenReturn(TEST_ACCESS_TOKEN_JTI);

    Optional<String> result =
        tokenStateStore.findAccessTokenJtiByRefreshToken(TEST_REFRESH_TOKEN_EXISTING);

    assertTrue(result.isPresent());
    assertEquals(TEST_ACCESS_TOKEN_JTI, result.orElseThrow());
  }

  @Test
  void unlinkRefreshTokenDeletesImmediatelyWithoutTransaction() {
    tokenStateStore.unlinkRefreshToken(TEST_REFRESH_TOKEN_EXISTING);

    verify(redisTemplate)
        .delete(tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(TEST_REFRESH_TOKEN_EXISTING));
  }

  @Test
  void unlinkRefreshTokenDeletesAfterCommitWhenTransactionIsActive() {
    TransactionSynchronizationManager.initSynchronization();

    tokenStateStore.unlinkRefreshToken(TEST_REFRESH_TOKEN_EXISTING);

    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations()) {
      synchronization.afterCommit();
    }

    verify(redisTemplate)
        .delete(tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(TEST_REFRESH_TOKEN_EXISTING));
  }

  @Test
  void isAccessTokenRevokedFailsClosedWhenRedisIsUnavailable() {
    when(redisTemplate.hasKey(
            tokenStateSupport.buildAccessTokenRevocationKey(TEST_ACCESS_TOKEN_JTI)))
        .thenThrow(new IllegalStateException(TEST_RATE_LIMIT_STORE_UNAVAILABLE));

    boolean revoked = tokenStateStore.isAccessTokenRevoked(TEST_ACCESS_TOKEN_JTI);

    assertTrue(revoked);
    assertEquals(1.0, meterRegistry.get(METRIC_AUTH_TOKEN_STATE_UNAVAILABLE).counter().count());
  }
}
