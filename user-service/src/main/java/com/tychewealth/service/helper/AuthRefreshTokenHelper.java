package com.tychewealth.service.helper;

import static com.tychewealth.constants.AuthConstants.REFRESH_TOKEN_BYTE_LENGTH;

import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthRefreshTokenHelper {

  private final RefreshTokenRepository refreshTokenRepository;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long refreshTokenTtlSeconds;

  public AuthRefreshTokenHelper(
      RefreshTokenRepository refreshTokenRepository,
      @Value("${app.auth.jwt.refresh-token-ttl-seconds:1209600}") long refreshTokenTtlSeconds) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
  }

  public void saveToken(UserEntity user, String token, Instant expiresAt) {
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();

    refreshToken.setUser(user);
    refreshToken.setToken(token);
    refreshToken.setExpiresAt(expiresAt);
    refreshToken.setRevoked(false);

    refreshTokenRepository.save(refreshToken);
  }

  @Transactional
  public int revokeActiveTokensByUserId(Long userId) {
    return refreshTokenRepository.revokeActiveTokensByUserId(userId);
  }

  @Transactional
  public boolean revokeToken(String token) {
    return refreshTokenRepository
        .findByToken(token)
        .map(
            refreshToken -> {
              if (refreshToken.isRevoked()) {
                return false;
              }
              refreshToken.setRevoked(true);
              refreshTokenRepository.save(refreshToken);
              return true;
            })
        .orElse(false);
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
