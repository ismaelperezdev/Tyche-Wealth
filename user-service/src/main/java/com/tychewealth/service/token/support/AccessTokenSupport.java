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
import java.util.Date;
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
      throw unauthorizedException();
    }

    return parseSubjectAsLong(claims);
  }

  public Long extractVerifyEmailUserId(String token) {
    return extractUserIdForPurpose(token, VERIFY_REGISTRATION_TOKEN_PURPOSE);
  }

  public Long extractVerifyLoginDeviceUserId(String token) {
    return extractUserIdForPurpose(token, VERIFY_LOGIN_DEVICE_TOKEN_PURPOSE);
  }

  public Instant extractExpiration(String token) {
    Date expiration = parseClaimsOrThrowUnauthorized(token).getExpiration();
    if (expiration == null) {
      throw unauthorizedException();
    }

    return expiration.toInstant();
  }

  public String extractTokenId(String token) {
    String tokenId = parseClaimsOrThrowUnauthorized(token).getId();

    if (!StringUtils.hasText(tokenId)) {
      throw unauthorizedException();
    }
    return tokenId;
  }

  private Long extractUserIdForPurpose(String token, String expectedPurpose) {
    Claims claims = parseClaimsOrThrowUnauthorized(token);
    String purpose = claims.get(TOKEN_PURPOSE_CLAIM, String.class);

    if (!expectedPurpose.equals(purpose)) {
      throw unauthorizedException();
    }

    try {
      return Long.valueOf(claims.getSubject());
    } catch (IllegalArgumentException ex) {
      throw unauthorizedException();
    }
  }

  private Claims parseClaimsOrThrowUnauthorized(String token) {
    try {
      return parseClaims(token);
    } catch (JwtException | IllegalArgumentException ex) {
      throw unauthorizedException();
    }
  }

  private Long parseSubjectAsLong(Claims claims) {
    try {
      return Long.valueOf(claims.getSubject());
    } catch (IllegalArgumentException ex) {
      throw unauthorizedException();
    }
  }

  private AuthException unauthorizedException() {
    log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
    return new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
  }
}
