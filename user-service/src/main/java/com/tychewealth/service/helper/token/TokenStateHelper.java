package com.tychewealth.service.helper.token;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_AUTHORIZATION_HEADER_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenStateHelper {

  private static final String ACCESS_TOKEN_REVOCATION_KEY_PREFIX =
      "auth:access-token:blacklist:jti:";
  private static final String REFRESH_TOKEN_ACCESS_TOKEN_LINK_KEY_PREFIX =
      "auth:refresh-token:access-token-jti:";

  private final RedisTemplate<String, String> redisTemplate;
  private final AccessTokenHelper accessTokenHelper;

  public void revokeAccessTokenIfPresent(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return;
    }

    String accessToken = extractBearerToken(authorizationHeader);
    String tokenId = accessTokenHelper.extractTokenId(accessToken);
    Instant expiresAt = accessTokenHelper.extractExpiration(accessToken);
    revokeAccessToken(tokenId, expiresAt);
  }

  public void revokeAccessToken(String tokenId, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isZero() || ttl.isNegative()) {
      return;
    }

    redisTemplate.opsForValue().set(buildAccessTokenRevocationKey(tokenId), "revoked", ttl);
  }

  public boolean isAccessTokenRevoked(String tokenId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(buildAccessTokenRevocationKey(tokenId)));
  }

  public void linkRefreshTokenToAccessToken(
      String refreshToken, String accessTokenJti, Instant refreshTokenExpiresAt) {
    Duration ttl = Duration.between(Instant.now(), refreshTokenExpiresAt);
    if (ttl.isZero() || ttl.isNegative()) {
      return;
    }

    redisTemplate
        .opsForValue()
        .set(buildRefreshTokenAccessTokenLinkKey(refreshToken), accessTokenJti, ttl);
  }

  public Optional<String> findAccessTokenJtiByRefreshToken(String refreshToken) {
    return Optional.ofNullable(
        redisTemplate.opsForValue().get(buildRefreshTokenAccessTokenLinkKey(refreshToken)));
  }

  public void unlinkRefreshToken(String refreshToken) {
    redisTemplate.delete(buildRefreshTokenAccessTokenLinkKey(refreshToken));
  }

  public String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null
        || !authorizationHeader.regionMatches(
            true, 0, TOKEN_TYPE_BEARER_PREFIX, 0, TOKEN_TYPE_BEARER_PREFIX.length())) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_AUTHORIZATION_HEADER_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }

    String token = authorizationHeader.substring(TOKEN_TYPE_BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_AUTHORIZATION_HEADER_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }

    return token;
  }

  private String buildAccessTokenRevocationKey(String tokenId) {
    return ACCESS_TOKEN_REVOCATION_KEY_PREFIX + tokenId;
  }

  private String buildRefreshTokenAccessTokenLinkKey(String refreshToken) {
    return REFRESH_TOKEN_ACCESS_TOKEN_LINK_KEY_PREFIX + sha256(refreshToken);
  }

  private String sha256(String refreshToken) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] hash = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }
}
