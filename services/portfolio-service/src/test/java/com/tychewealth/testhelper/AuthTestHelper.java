package com.tychewealth.testhelper;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_JWT_SECRET;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class AuthTestHelper {

  private static final SecretKey SIGNING_KEY =
      Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));

  private AuthTestHelper() {}

  public static String createAuthorizationHeader(long userId) {
    return TOKEN_TYPE_BEARER_PREFIX + createAccessToken(userId);
  }

  public static String createAccessToken(long userId) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusSeconds(900);

    return Jwts.builder()
        .subject(String.valueOf(userId))
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(SIGNING_KEY, Jwts.SIG.HS256)
        .compact();
  }
}
