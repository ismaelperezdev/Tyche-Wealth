package com.tychewealth.service.helper.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenRevocationHelper {

  private static final String KEY_PREFIX = "auth:access-token:blacklist:";

  private final RedisTemplate<String, String> redisTemplate;

  public AccessTokenRevocationHelper(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void revoke(String token, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isZero() || ttl.isNegative()) {
      return;
    }

    redisTemplate.opsForValue().set(buildKey(token), "revoked", ttl);
  }

  public boolean isRevoked(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(token)));
  }

  private String buildKey(String token) {
    return KEY_PREFIX + sha256(token);
  }

  private String sha256(String token) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] hash = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }
}
