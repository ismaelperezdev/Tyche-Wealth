package com.tychewealth.service.helper;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.service.token.AuthTokenPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenHelper {

  private final SecretKey signingKey;
  private final long accessTokenTtlSeconds;

  public AuthTokenHelper(
      @Value("${app.auth.jwt.secret}") String jwtSecret,
      @Value("${app.auth.jwt.access-token-ttl-seconds:3600}") long accessTokenTtlSeconds) {

    if (accessTokenTtlSeconds <= 0)
      throw new IllegalArgumentException(
          "app.auth.jwt.access-token-ttl-seconds must be greater than 0");

    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
  }

  public AuthTokenPayload generateAccessToken(UserEntity user) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusSeconds(accessTokenTtlSeconds);

    String token =
        Jwts.builder()
            .header()
            .type("JWT")
            .and()
            .subject(String.valueOf(user.getId()))
            .claim("email", user.getEmail())
            .claim("username", user.getUsername())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();

    return new AuthTokenPayload(TOKEN_TYPE_BEARER, token, accessTokenTtlSeconds);
  }
}
