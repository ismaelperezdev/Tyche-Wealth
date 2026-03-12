package com.tychewealth.service.helper;

import static com.tychewealth.constants.AuthConstants.REFRESH_TOKEN_BYTE_LENGTH;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_REFRESH_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REFRESH_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

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

@Slf4j
@Component
public class AuthRefreshTokenHelper {

  private final RefreshTokenRepository refreshTokenRepository;
  private final AuthMetrics authMetrics;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long refreshTokenTtlSeconds;

  public AuthRefreshTokenHelper(
      RefreshTokenRepository refreshTokenRepository,
      AuthMetrics authMetrics,
      @Value("${app.auth.jwt.refresh-token-ttl-seconds:1209600}") long refreshTokenTtlSeconds) {
    if (refreshTokenTtlSeconds <= 0) {
      throw new IllegalArgumentException("Refresh token TTL must be positive");
    }
    this.refreshTokenRepository = refreshTokenRepository;
    this.authMetrics = authMetrics;
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
  }

  @Transactional
  public void saveToken(UserEntity user, String token, Instant expiresAt) {
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();

    refreshToken.setUser(user);
    refreshToken.setToken(token);
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
    int revokedCount = refreshTokenRepository.revokeTokenIfActive(token, Instant.now());
    authMetrics.recordTokensRevoked(revokedCount);

    if (revokedCount == 0) {
      throwInvalidRefreshToken();
    }

    return findByToken(token).orElseThrow(this::buildInvalidRefreshTokenException);
  }

  public Optional<RefreshTokenEntity> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
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

  public String generateRefreshToken() {
    byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  public Instant calculateRefreshTokenExpiration() {
    return Instant.now().plusSeconds(refreshTokenTtlSeconds);
  }
}
