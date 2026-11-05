package com.tychewealth.security.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** Checks user-service's shared Redis access-token revocation state. */
@Component
@Slf4j
public class TokenStateSupport {

  private static final String ACCESS_TOKEN_REVOCATION_KEY_PREFIX =
      "auth:access-token:blacklist:jti:";

  private final RedisTemplate<String, String> redisTemplate;

  public TokenStateSupport(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean isAccessTokenRevoked(String tokenId) {
    try {
      return redisTemplate.hasKey(ACCESS_TOKEN_REVOCATION_KEY_PREFIX + tokenId);
    } catch (RuntimeException exception) {
      log.error("Redis access-token revocation check failed; rejecting token", exception);
      return true;
    }
  }

  public String extractBearerToken(String authorizationHeader) {
    String bearerPrefix = "Bearer ";
    if (authorizationHeader == null
        || !authorizationHeader.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())) {
      throw new org.springframework.security.authentication.BadCredentialsException(
          "Invalid authorization header");
    }

    String token = authorizationHeader.substring(bearerPrefix.length()).trim();
    if (token.isEmpty()) {
      throw new org.springframework.security.authentication.BadCredentialsException(
          "Invalid authorization header");
    }
    return token;
  }
}
