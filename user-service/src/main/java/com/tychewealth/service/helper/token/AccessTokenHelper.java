package com.tychewealth.service.helper.token;

import static com.tychewealth.constants.AuthConstants.TOKEN_PURPOSE_CLAIM;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.AuthConstants.VERIFY_REGISTRATION_TOKEN_PURPOSE;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_ACCESS_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.token.AuthTokenPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AccessTokenHelper {

  private final SecretKey signingKey;
  private final long accessTokenTtlSeconds;
  private final long verifyEmailTokenTtlSeconds;

  public AccessTokenHelper(
      @Value("${app.auth.jwt.secret}") String jwtSecret,
      @Value("${app.auth.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
      @Value("${app.auth.jwt.verify-email-token-ttl-seconds:900}")
          long verifyEmailTokenTtlSeconds) {

    if (accessTokenTtlSeconds <= 0) {
      throw new IllegalArgumentException(
          "app.auth.jwt.access-token-ttl-seconds must be greater than 0");
    }
    if (verifyEmailTokenTtlSeconds <= 0) {
      throw new IllegalArgumentException(
          "app.auth.jwt.verify-email-token-ttl-seconds must be greater than 0");
    }

    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    this.verifyEmailTokenTtlSeconds = verifyEmailTokenTtlSeconds;
  }

  public AuthTokenPayload generateAccessToken(UserEntity user) {
    return generateToken(user, accessTokenTtlSeconds, null);
  }

  public AuthTokenPayload generateVerifyEmailToken(UserEntity user) {
    return generateToken(user, verifyEmailTokenTtlSeconds, VERIFY_REGISTRATION_TOKEN_PURPOSE);
  }

  public Long extractVerifyEmailUserId(String token) {
    try {
      Claims claims = parseClaims(token);
      String purpose = claims.get(TOKEN_PURPOSE_CLAIM, String.class);
      if (!VERIFY_REGISTRATION_TOKEN_PURPOSE.equals(purpose)) {
        throw new IllegalArgumentException("Token is not intended for email verification");
      }
      return Long.valueOf(claims.getSubject());
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }
  }

  private AuthTokenPayload generateToken(UserEntity user, long ttlSeconds, String purpose) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
    String jti = UUID.randomUUID().toString();

    var builder =
        Jwts.builder()
            .header()
            .type("JWT")
            .and()
            .subject(String.valueOf(user.getId()))
            .id(jti)
            .claim("email", user.getEmail())
            .claim("username", user.getUsername())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt));

    if (StringUtils.hasText(purpose)) {
      builder.claim(TOKEN_PURPOSE_CLAIM, purpose);
    }

    String token = builder.signWith(signingKey, Jwts.SIG.HS256).compact();

    return new AuthTokenPayload(TOKEN_TYPE_BEARER, token, ttlSeconds, jti);
  }

  public Long extractUserId(String token) {
    try {
      Claims claims = parseClaims(token);
      String purpose = claims.get(TOKEN_PURPOSE_CLAIM, String.class);

      if (StringUtils.hasText(purpose)) {
        throw new IllegalArgumentException("Token with purpose is not valid for bearer auth");
      }

      return Long.valueOf(claims.getSubject());
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }
  }

  public Instant extractExpiration(String token) {
    try {
      return parseClaims(token).getExpiration().toInstant();
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }
  }

  public String extractTokenId(String token) {
    try {
      String tokenId = parseClaims(token).getId();

      if (!StringUtils.hasText(tokenId)) {
        throw new IllegalArgumentException("Access token is missing jti");
      }
      return tokenId;

    } catch (JwtException | IllegalArgumentException ex) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }
  }

  private Claims parseClaims(String token) {
    JwtParser jwtParser = Jwts.parser().verifyWith(signingKey).build();
    return jwtParser.parseSignedClaims(token).getPayload();
  }
}
