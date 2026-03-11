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

  /**
   * Create a new AuthRefreshTokenHelper configured with the refresh token repository, metrics collector, and token TTL.
   *
   * @param refreshTokenRepository repository used to persist and query refresh token entities
   * @param authMetrics            metrics collector used to record token issuance, revocation, and failures
   * @param refreshTokenTtlSeconds time-to-live for refresh tokens in seconds; must be greater than zero
   * @throws IllegalArgumentException if {@code refreshTokenTtlSeconds} is less than or equal to zero
   */
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

  /**
   * Persist a new refresh token for the user and record issuance metrics.
   *
   * Creates and stores a refresh token for the given user with the provided token string and expiration time,
   * and increments the issued-token count in metrics.
   *
   * @param user the user owning the refresh token
   * @param token the opaque refresh token string
   * @param expiresAt the instant when the token expires
   */
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

  /**
   * Revokes all active refresh tokens belonging to the specified user.
   *
   * @param userId the ID of the user whose active refresh tokens will be revoked
   * @return the number of refresh tokens revoked for the user
   */
  @Transactional
  public int revokeActiveTokensByUserId(Long userId) {
    int revokedCount = refreshTokenRepository.revokeActiveTokensByUserId(userId, Instant.now());
    authMetrics.recordTokensRevoked(revokedCount);
    return revokedCount;
  }

  /**
   * Validates the provided refresh token by revoking it if active and returns the stored token entity.
   *
   * @param token the refresh token string presented for validation
   * @return the persisted RefreshTokenEntity associated with the provided token
   * @throws AuthException if the token is invalid or not active
   * @throws IllegalStateException if the token cannot be found after successful revocation
   */
  @Transactional
  public RefreshTokenEntity validateRefreshToken(String token) {
    int revokedCount = refreshTokenRepository.revokeTokenIfActive(token, Instant.now());
    authMetrics.recordTokensRevoked(revokedCount);

    if (revokedCount == 0) {
      throwInvalidRefreshToken();
    }

    return findByToken(token)
        .orElseThrow(
            () ->
                new IllegalStateException("Refresh token disappeared after successful revocation"));
  }

  /**
   * Look up a refresh token entity by its token string.
   *
   * @param token the refresh token string to look up
   * @return an Optional containing the matching RefreshTokenEntity if found, or an empty Optional otherwise
   */
  public Optional<RefreshTokenEntity> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  /**
   * Record a refresh-token failure and raise an authorization error for an invalid refresh token.
   *
   * @throws AuthException indicating the refresh token is invalid with HTTP 401 Unauthorized
   */
  private void throwInvalidRefreshToken() {
    log.warn(REQUEST_CONFLICT, AUTH, REFRESH_TOKEN_ACTION, INVALID_REFRESH_TOKEN_MESSAGE);
    authMetrics.recordRefreshFailure();

    throw new AuthException(
        ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID, null, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Generates a new refresh token string suitable for use in URLs.
   *
   * The token is produced from cryptographically secure random bytes and encoded using URL-safe
   * Base64 without padding.
   *
   * @return the refresh token as a URL-safe Base64 string
   */
  public String generateRefreshToken() {
    byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  public Instant calculateRefreshTokenExpiration() {
    return Instant.now().plusSeconds(refreshTokenTtlSeconds);
  }
}
