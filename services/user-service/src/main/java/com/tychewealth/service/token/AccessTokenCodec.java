package com.tychewealth.service.token;

import static com.tychewealth.constants.AuthConstants.FORGOT_PASSWORD_TOKEN_PURPOSE;
import static com.tychewealth.constants.AuthConstants.TOKEN_PURPOSE_CLAIM;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.AuthConstants.VERIFY_LOGIN_DEVICE_TOKEN_PURPOSE;
import static com.tychewealth.constants.AuthConstants.VERIFY_REGISTRATION_TOKEN_PURPOSE;
import static com.tychewealth.constants.CommonConstants.FIELD_EMAIL;
import static com.tychewealth.constants.CommonConstants.FIELD_USERNAME;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_ACCESS_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.token.support.AccessTokenSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Encodes and decodes the JWT credentials used by the authentication workflows.
 *
 * <p>Issues access, email-verification, trusted-device-verification, and password-recovery tokens
 * with purpose-specific lifetimes and claims. Delegates cryptographic parsing to {@link
 * AccessTokenSupport} and rejects tokens whose purpose, identifier, subject, or signature does not
 * match the expected access-token contract.
 */
@Slf4j
@Component
public class AccessTokenCodec {

  private final AccessTokenSupport accessTokenSupport;
  private final long accessTokenTtlSeconds;
  private final long verifyEmailTokenTtlSeconds;
  private final long verifyLoginDeviceTokenTtlSeconds;
  private final long forgotPasswordTokenTtlSeconds;

  public AccessTokenCodec(
      AccessTokenSupport accessTokenSupport,
      @Value("${app.auth.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
      @Value("${app.auth.jwt.verify-email-token-ttl-seconds:86400}")
          long verifyEmailTokenTtlSeconds,
      @Value("${app.auth.jwt.verify-login-device-token-ttl-seconds:86400}")
          long verifyLoginDeviceTokenTtlSeconds,
      @Value("${app.auth.jwt.forgot-password-token-ttl-seconds:1800}")
          long forgotPasswordTokenTtlSeconds) {

    this.accessTokenSupport = accessTokenSupport;
    this.accessTokenTtlSeconds =
        accessTokenSupport.requirePositiveTtl(
            accessTokenTtlSeconds, "app.auth.jwt.access-token-ttl-seconds");
    this.verifyEmailTokenTtlSeconds =
        accessTokenSupport.requirePositiveTtl(
            verifyEmailTokenTtlSeconds, "app.auth.jwt.verify-email-token-ttl-seconds");
    this.verifyLoginDeviceTokenTtlSeconds =
        accessTokenSupport.requirePositiveTtl(
            verifyLoginDeviceTokenTtlSeconds, "app.auth.jwt.verify-login-device-token-ttl-seconds");
    this.forgotPasswordTokenTtlSeconds =
        accessTokenSupport.requirePositiveTtl(
            forgotPasswordTokenTtlSeconds, "app.auth.jwt.forgot-password-token-ttl-seconds");
  }

  public AuthTokenDto generateToken(UserEntity user, AccessTokenType tokenType) {
    if (tokenType == null) {
      throw new IllegalArgumentException("generateToken requires a non-null tokenType");
    }
    if (user == null) {
      throw new IllegalArgumentException("generateToken requires a non-null user");
    }
    if (user.getId() == null) {
      throw new IllegalArgumentException("generateToken requires a non-null user id");
    }

    long ttlSeconds = resolveTtlSeconds(tokenType);
    String purpose = resolvePurpose(tokenType);
    Instant issuedAt = Instant.now();
    String jti = createTokenId();
    boolean includeSensitiveClaims = tokenType == AccessTokenType.ACCESS;

    String token =
        buildJwtBuilder(user, issuedAt, ttlSeconds, jti, purpose, includeSensitiveClaims)
            .signWith(accessTokenSupport.signingKey(), Jwts.SIG.HS256)
            .compact();

    return new AuthTokenDto(TOKEN_TYPE_BEARER, token, ttlSeconds, jti);
  }

  public Long extractUserId(String token) {
    return parseAccessToken(token).userId();
  }

  public Long extractVerifyEmailUserId(String token) {
    return accessTokenSupport.extractVerifyEmailUserId(token);
  }

  public Long extractVerifyLoginDeviceUserId(String token) {
    return accessTokenSupport.extractVerifyLoginDeviceUserId(token);
  }

  public Instant extractExpiration(String token) {
    return accessTokenSupport.extractExpiration(token);
  }

  public String extractTokenId(String token) {
    return parseAccessToken(token).tokenId();
  }

  public ParsedAccessToken parseAccessToken(String token) {
    Claims claims = accessTokenSupport.parseValidatedClaims(token);
    String purpose = claims.get(TOKEN_PURPOSE_CLAIM, String.class);
    if (StringUtils.hasText(purpose)) {
      throw unauthorizedException();
    }

    String tokenId = claims.getId();
    if (!StringUtils.hasText(tokenId)) {
      throw unauthorizedException();
    }

    try {
      return new ParsedAccessToken(Long.valueOf(claims.getSubject()), tokenId);
    } catch (IllegalArgumentException ex) {
      throw unauthorizedException();
    }
  }

  private long resolveTtlSeconds(AccessTokenType tokenType) {
    return switch (tokenType) {
      case ACCESS -> accessTokenTtlSeconds;
      case VERIFY_EMAIL -> verifyEmailTokenTtlSeconds;
      case VERIFY_LOGIN_DEVICE -> verifyLoginDeviceTokenTtlSeconds;
      case FORGOT_PASSWORD -> forgotPasswordTokenTtlSeconds;
    };
  }

  private String resolvePurpose(AccessTokenType tokenType) {
    return switch (tokenType) {
      case ACCESS -> null;
      case VERIFY_EMAIL -> VERIFY_REGISTRATION_TOKEN_PURPOSE;
      case VERIFY_LOGIN_DEVICE -> VERIFY_LOGIN_DEVICE_TOKEN_PURPOSE;
      case FORGOT_PASSWORD -> FORGOT_PASSWORD_TOKEN_PURPOSE;
    };
  }

  private JwtBuilder buildJwtBuilder(
      UserEntity user,
      Instant issuedAt,
      long ttlSeconds,
      String jti,
      String purpose,
      boolean includeSensitiveClaims) {
    Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
    JwtBuilder builder =
        Jwts.builder()
            .header()
            .type("JWT")
            .and()
            .subject(String.valueOf(user.getId()))
            .id(jti)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt));

    if (includeSensitiveClaims) {
      builder.claim(FIELD_EMAIL, user.getEmail()).claim(FIELD_USERNAME, user.getUsername());
    }

    if (StringUtils.hasText(purpose)) {
      builder.claim(TOKEN_PURPOSE_CLAIM, purpose);
    }

    return builder;
  }

  private String createTokenId() {
    return UUID.randomUUID().toString();
  }

  private AuthException unauthorizedException() {
    log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
    return new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
  }

  public record ParsedAccessToken(Long userId, String tokenId) {}
}
