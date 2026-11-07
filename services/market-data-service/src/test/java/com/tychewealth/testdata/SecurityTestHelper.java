package com.tychewealth.testdata;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;

public final class SecurityTestHelper {

  private static final String JWT_SECRET = "test-jwt-secret-0123456789abcdef";

  private SecurityTestHelper() {}

  public static String basicAuth() {
    String credentials = "prometheus-scraper:test-prometheus-password";
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  public static String accessToken(String tokenId) {
    SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject("42")
        .id(tokenId)
        .issuedAt(Date.from(Instant.now().minusSeconds(1)))
        .expiration(Date.from(Instant.now().plusSeconds(900)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }
}
