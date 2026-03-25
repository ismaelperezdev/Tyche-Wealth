package com.tychewealth.service.helper.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.service.token.AuthTokenPayload;
import com.tychewealth.testdata.EntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessTokenHelperTest {

  private static final String TEST_JWT_SECRET =
      "0123456789012345678901234567890123456789012345678901234567890123";
  private static final long TEST_ACCESS_TOKEN_TTL_SECONDS = 900;
  private static final long TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS = 86400;
  private static final long TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS = 1800;

  private AccessTokenHelper accessTokenHelper;

  @BeforeEach
  void setUp() {
    accessTokenHelper =
        new AccessTokenHelper(
            TEST_JWT_SECRET,
            TEST_ACCESS_TOKEN_TTL_SECONDS,
            TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS);
  }

  @Test
  void extractUserIdReturnsSubjectForNormalAccessToken() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    Long userId =
        accessTokenHelper.extractUserId(accessTokenHelper.generateAccessToken(user).accessToken());

    assertEquals(42L, userId);
  }

  @Test
  void extractUserIdRejectsVerifyRegistrationToken() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    String verifyRegistrationToken = accessTokenHelper.generateVerifyEmailToken(user).accessToken();

    assertThrows(
        AuthException.class, () -> accessTokenHelper.extractUserId(verifyRegistrationToken));
  }

  @Test
  void generateVerifyEmailTokenUsesTwentyFourHourTtl() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    AuthTokenPayload verifyRegistrationToken = accessTokenHelper.generateVerifyEmailToken(user);

    assertEquals(TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS, verifyRegistrationToken.expiresIn());
  }

  @Test
  void generateVerifyLoginDeviceTokenUsesDedicatedTtl() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    AuthTokenPayload verifyLoginDeviceToken =
        accessTokenHelper.generateVerifyLoginDeviceToken(user);

    assertEquals(TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS, verifyLoginDeviceToken.expiresIn());
  }
}
