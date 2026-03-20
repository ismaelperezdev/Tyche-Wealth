package com.tychewealth.service.helper.token;

import static com.tychewealth.constants.AuthConstants.REFRESH_TOKEN_BYTE_LENGTH;
import static com.tychewealth.constants.AuthConstants.TOKEN_LINK_MAX_ATTEMPTS;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_REFRESH_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REFRESH_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.USER_ID;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.service.monitoring.AuthMetrics;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AuthRefreshTokenHelper {

  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenStateHelper tokenStateHelper;
  private final AuthMetrics authMetrics;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long refreshTokenTtlSeconds;
  private final String refreshTokenPepper;

  public AuthRefreshTokenHelper(
      RefreshTokenRepository refreshTokenRepository,
      TokenStateHelper tokenStateHelper,
      AuthMetrics authMetrics,
      @Value("${app.auth.jwt.refresh-token-ttl-seconds:1209600}") long refreshTokenTtlSeconds,
      @Value("${app.auth.jwt.refresh-token-pepper}") String refreshTokenPepper) {
    if (refreshTokenTtlSeconds <= 0) {
      throw new IllegalArgumentException("Refresh token TTL must be positive");
    }
    if (!StringUtils.hasText(refreshTokenPepper)) {
      throw new IllegalArgumentException("Refresh token pepper must be configured");
    }
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenStateHelper = tokenStateHelper;
    this.authMetrics = authMetrics;
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    this.refreshTokenPepper = refreshTokenPepper;
  }

  public record LinkedRefreshToken(String token, Instant expiresAt) {}

  @Transactional
  public LinkedRefreshToken saveToken(UserEntity user, String accessTokenJti, String action) {
    String refreshToken = generateRefreshToken();
    Instant refreshTokenExpiresAt = calculateRefreshTokenExpiration();
    persistRefreshToken(user, refreshToken, refreshTokenExpiresAt);
    linkRefreshTokenOrRollback(
        user.getId(), refreshToken, accessTokenJti, refreshTokenExpiresAt, action);
    return new LinkedRefreshToken(refreshToken, refreshTokenExpiresAt);
  }

  private void persistRefreshToken(UserEntity user, String token, Instant expiresAt) {
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();

    refreshToken.setUser(user);
    refreshToken.setToken(hashRefreshToken(token));
    refreshToken.setExpiresAt(expiresAt);
    refreshToken.setRevoked(false);

    refreshTokenRepository.save(refreshToken);
    authMetrics.recordTokensIssued(1);
  }

  @Transactional
  public int revokeActiveTokensByUserId(Long userId) {
    int revokedCount = refreshTokenRepository.revokeActiveTokensByUserId(userId, Instant.now());
    authMetrics.recordTokensRevoked(revokedCount);
    return revokedCount;
  }

  @Transactional
  public RefreshTokenEntity validateRefreshToken(String token) {
    int revokedCount =
        refreshTokenRepository.revokeTokenIfActive(hashRefreshToken(token), Instant.now());
    authMetrics.recordTokensRevoked(revokedCount);

    if (revokedCount == 0) {
      throwInvalidRefreshToken();
    }

    return findByToken(token).orElseThrow(this::buildInvalidRefreshTokenException);
  }

  public Optional<RefreshTokenEntity> findByToken(String token) {
    return refreshTokenRepository.findByToken(hashRefreshToken(token));
  }

  public String hashRefreshToken(String token) {
    return com.tychewealth.utils.Utils.sha256Hex(token, refreshTokenPepper);
  }

  private void throwInvalidRefreshToken() {
    log.warn(REQUEST_CONFLICT, AUTH, REFRESH_TOKEN_ACTION, INVALID_REFRESH_TOKEN_MESSAGE);
    authMetrics.recordRefreshFailure();

    throw buildInvalidRefreshTokenException();
  }

  private AuthException buildInvalidRefreshTokenException() {
    return new AuthException(
        ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID, null, HttpStatus.UNAUTHORIZED);
  }

  private String generateRefreshToken() {
    byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  private Instant calculateRefreshTokenExpiration() {
    return Instant.now().plusSeconds(refreshTokenTtlSeconds);
  }

  private void linkRefreshTokenOrRollback(
      Long userId,
      String refreshToken,
      String accessTokenJti,
      Instant refreshTokenExpiresAt,
      String action) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= TOKEN_LINK_MAX_ATTEMPTS; attempt++) {
      try {
        tokenStateHelper.linkRefreshTokenToAccessToken(
            refreshToken, accessTokenJti, refreshTokenExpiresAt);
        return;
      } catch (RuntimeException ex) {
        lastFailure = ex;
        log.warn(
            LogConstants.REQUEST_CONFLICT + USER_ID + " attempt={}",
            AUTH,
            action,
            "failed to link refresh token to access token state",
            userId,
            attempt,
            ex);
      }
    }

    refreshTokenRepository.deleteByToken(hashRefreshToken(refreshToken));
    authMetrics.recordTokenStateUnavailable();
    AuthException exception =
        new AuthException(
            ErrorDefinition.GENERIC_INTERNAL_ERROR, null, HttpStatus.SERVICE_UNAVAILABLE);
    exception.initCause(lastFailure);
    throw exception;
  }
}
