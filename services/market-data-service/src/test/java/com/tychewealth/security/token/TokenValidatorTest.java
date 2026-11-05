package com.tychewealth.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;

class TokenValidatorTest {

  private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";

  private final RedisTemplate<String, String> redisTemplate = org.mockito.Mockito.mock();
  private TokenValidator tokenValidator;

  @BeforeEach
  void setUp() {
    AccessTokenSupport accessTokenSupport = new AccessTokenSupport(JWT_SECRET);
    tokenValidator = new TokenValidator(accessTokenSupport, new TokenStateSupport(redisTemplate));
  }

  @Test
  void validatesAccessTokenAndReturnsUserId() {
    String token = accessToken(Instant.now().plusSeconds(900), null, "42");
    when(redisTemplate.hasKey("auth:access-token:blacklist:jti:jti-123")).thenReturn(false);

    assertThat(tokenValidator.validateAndExtractUserId("Bearer " + token)).isEqualTo(42L);
  }

  @Test
  void rejectsTokenWithNonAccessPurpose() {
    String token = accessToken(Instant.now().plusSeconds(900), "forgot-password", "42");

    assertThatThrownBy(() -> tokenValidator.validateAndExtractUserId("Bearer " + token))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void rejectsTokenWithPurposeClaimOfUnexpectedType() {
    String token = accessTokenWithPurposeClaim();

    assertThatThrownBy(() -> tokenValidator.validateAndExtractUserId("Bearer " + token))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void rejectsTokenWithoutSubject() {
    String token = accessToken(Instant.now().plusSeconds(900), null, null);

    assertThatThrownBy(() -> tokenValidator.validateAndExtractUserId("Bearer " + token))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void rejectsExpiredToken() {
    String token = accessToken(Instant.now().minusSeconds(1), null, "42");

    assertThatThrownBy(() -> tokenValidator.validateAndExtractUserId("Bearer " + token))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void rejectsRevokedToken() {
    String token = accessToken(Instant.now().plusSeconds(900), null, "42");
    when(redisTemplate.hasKey("auth:access-token:blacklist:jti:jti-123")).thenReturn(true);

    assertThatThrownBy(() -> tokenValidator.validateAndExtractUserId("Bearer " + token))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void rejectsTokenWhenRedisRevocationStateIsUnavailable() {
    String token = accessToken(Instant.now().plusSeconds(900), null, "42");
    doThrow(new IllegalStateException("Redis unavailable"))
        .when(redisTemplate)
        .hasKey("auth:access-token:blacklist:jti:jti-123");

    assertThatThrownBy(() -> tokenValidator.validateAndExtractUserId("Bearer " + token))
        .isInstanceOf(BadCredentialsException.class);
    verify(redisTemplate).hasKey("auth:access-token:blacklist:jti:jti-123");
  }

  private String accessToken(Instant expiresAt, String purpose, String subject) {
    SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    var builder =
        Jwts.builder()
            .subject(subject)
            .id("jti-123")
            .issuedAt(Date.from(Instant.now().minusSeconds(1)))
            .expiration(Date.from(expiresAt));
    if (purpose != null) {
      builder.claim("purpose", purpose);
    }
    return builder.signWith(key, Jwts.SIG.HS256).compact();
  }

  private String accessTokenWithPurposeClaim() {
    SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject("42")
        .id("jti-123")
        .claim("purpose", 42)
        .issuedAt(Date.from(Instant.now().minusSeconds(1)))
        .expiration(Date.from(Instant.now().plusSeconds(900)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }
}
