package com.tychewealth.service.token.support;

import static com.tychewealth.constants.AuthConstants.TOKEN_PURPOSE_CLAIM;
import static com.tychewealth.constants.AuthConstants.VERIFY_LOGIN_DEVICE_TOKEN_PURPOSE;
import static com.tychewealth.constants.AuthConstants.VERIFY_REGISTRATION_TOKEN_PURPOSE;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_ACCESS_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AccessTokenSupport {

  private final SecretKey signingKey;

  public AccessTokenSupport(@Value("${app.auth.jwt.secret}") String jwtSecret) {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public long requirePositiveTtl(long ttlSeconds, String propertyName) {
    if (ttlSeconds <= 0) {
      throw new IllegalArgumentException(propertyName + " must be greater than 0");
    }
    return ttlSeconds;
  }

  public Claims parseClaims(String token) {
    JwtParser jwtParser = Jwts.parser().verifyWith(signingKey).build();
    return jwtParser.parseSignedClaims(token).getPayload();
  }

  public SecretKey signingKey() {
    return signingKey;
  }

  public Long extractUserId(String token) {
    Claims claims = parseClaimsOrThrowUnauthorized(token);
    String purpose = claims.get(TOKEN_PURPOSE_CLAIM, String.class);

    if (StringUtils.hasText(purpose)) {
      throwUnauthorized();
    }

    try {
      return Long.valueOf(claims.getSubject());
    } catch (IllegalArgumentException ex) {
      throwUnauthorized();
      return null;
    }
  }

  public Long extractVerifyEmailUserId(String token) {
    return extractUserIdForPurpose(token, VERIFY_REGISTRATION_TOKEN_PURPOSE);
  }

  public Long extractVerifyLoginDeviceUserId(String token) {
    return extractUserIdForPurpose(token, VERIFY_LOGIN_DEVICE_TOKEN_PURPOSE);
  }

  public Instant extractExpiration(String token) {
    try {
      return parseClaimsOrThrowUnauthorized(token).getExpiration().toInstant();
    } catch (IllegalArgumentException ex) {
      throwUnauthorized();
      return null;
    }
  }

  public String extractTokenId(String token) {
    String tokenId = parseClaimsOrThrowUnauthorized(token).getId();

    if (!StringUtils.hasText(tokenId)) {
      throwUnauthorized();
    }
    return tokenId;
  }

  private Long extractUserIdForPurpose(String token, String expectedPurpose) {
    Claims claims = parseClaimsOrThrowUnauthorized(token);
    String purpose = claims.get(TOKEN_PURPOSE_CLAIM, String.class);

    if (!expectedPurpose.equals(purpose)) {
      throwUnauthorized();
    }

    try {
      return Long.valueOf(claims.getSubject());
    } catch (IllegalArgumentException ex) {
      throwUnauthorized();
      return null;
    }
  }

  private Claims parseClaimsOrThrowUnauthorized(String token) {
    try {
      return parseClaims(token);
    } catch (JwtException | IllegalArgumentException ex) {
      throwUnauthorized();
      return null;
    }
  }

  private void throwUnauthorized() {
    log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
    throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
  }
}
