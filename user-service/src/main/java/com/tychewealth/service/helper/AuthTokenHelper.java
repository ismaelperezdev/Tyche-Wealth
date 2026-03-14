package com.tychewealth.service.helper;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_ACCESS_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.INVALID_AUTHORIZATION_HEADER_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.token.AuthTokenPayload;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthTokenHelper {

  private final SecretKey signingKey;
  private final long accessTokenTtlSeconds;

  public AuthTokenHelper(
      @Value("${app.auth.jwt.secret}") String jwtSecret,
      @Value("${app.auth.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {

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

  public Long extractUserId(String authorizationHeader) {
    String token = extractBearerToken(authorizationHeader);

    try {
      String subject =
          Jwts.parser()
              .verifyWith(signingKey)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
      return Long.valueOf(subject);
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }
  }

  private String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_TYPE_BEARER_PREFIX)) {
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
}
