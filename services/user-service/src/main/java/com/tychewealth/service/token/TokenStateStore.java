package com.tychewealth.service.token;

import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.service.token.support.TokenStateSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class TokenStateStore {

  private final RedisTemplate<String, String> redisTemplate;
  private final AccessTokenCodec accessTokenCodec;
  private final AuthMetrics authMetrics;
  private final TokenStateSupport tokenStateSupport;

  public void revokeAccessTokenIfPresent(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return;
    }

    String accessToken = extractBearerToken(authorizationHeader);
    String tokenId = accessTokenCodec.extractTokenId(accessToken);
    Instant expiresAt = accessTokenCodec.extractExpiration(accessToken);
    revokeAccessToken(tokenId, expiresAt);
  }

  public void revokeAccessToken(String tokenId, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isZero() || ttl.isNegative()) {
      return;
    }

    redisTemplate
        .opsForValue()
        .set(tokenStateSupport.buildAccessTokenRevocationKey(tokenId), "revoked", ttl);
  }

  public boolean isAccessTokenRevoked(String tokenId) {
    return tokenStateSupport.isAccessTokenRevoked(redisTemplate, authMetrics, tokenId);
  }

  public void linkRefreshTokenToAccessToken(
      String refreshToken, String accessTokenJti, Instant refreshTokenExpiresAt) {
    Duration ttl = Duration.between(Instant.now(), refreshTokenExpiresAt);
    if (ttl.isZero() || ttl.isNegative()) {
      return;
    }

    redisTemplate
        .opsForValue()
        .set(
            tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(refreshToken),
            accessTokenJti,
            ttl);
  }

  public Optional<String> findAccessTokenJtiByRefreshToken(String refreshToken) {
    return tokenStateSupport.findAccessTokenJtiByRefreshToken(redisTemplate, refreshToken);
  }

  public void unlinkRefreshToken(String refreshToken) {
    String redisKey = tokenStateSupport.buildRefreshTokenAccessTokenLinkKey(refreshToken);
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      redisTemplate.delete(redisKey);
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              redisTemplate.delete(redisKey);
            } catch (RuntimeException ex) {
              authMetrics.incrementMetric(AuthMetricEnum.TOKEN_STATE_UNAVAILABLE);
            }
          }
        });
  }

  public String extractBearerToken(String authorizationHeader) {
    return tokenStateSupport.extractBearerToken(authorizationHeader);
  }
}
