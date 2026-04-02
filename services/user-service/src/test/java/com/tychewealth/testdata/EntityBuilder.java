package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_PEPPER;

import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.TrustedDeviceEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.utils.Utils;
import java.time.Instant;

public final class EntityBuilder {

  private EntityBuilder() {}

  public static UserEntity buildUser(String email, String username, String password) {
    UserEntity user = new UserEntity();
    user.setEmail(email);
    user.setUsername(username);
    user.setPassword(password);
    return user;
  }

  public static RefreshTokenEntity buildRefreshToken(
      String token, UserEntity user, Instant expiresAt, boolean revoked) {
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();
    refreshToken.setToken(Utils.hmacSha256Hex(token, TEST_REFRESH_TOKEN_PEPPER));
    refreshToken.setUser(user);
    refreshToken.setExpiresAt(expiresAt);
    refreshToken.setRevoked(revoked);
    return refreshToken;
  }

  public static TrustedDeviceEntity buildTrustedDevice(
      String token, UserEntity user, Instant expiresAt, Instant lastUsedAt) {
    TrustedDeviceEntity trustedDevice = new TrustedDeviceEntity();
    trustedDevice.setUser(user);
    trustedDevice.setTokenHash(Utils.sha256Hex(token));
    trustedDevice.setExpiresAt(expiresAt);
    trustedDevice.setLastUsedAt(lastUsedAt);
    return trustedDevice;
  }
}
