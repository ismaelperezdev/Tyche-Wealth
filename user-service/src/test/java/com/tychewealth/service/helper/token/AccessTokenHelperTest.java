package com.tychewealth.service.helper.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.token.support.AccessTokenSupport;
import com.tychewealth.testdata.EntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessTokenCodecTest {

  private static final String TEST_JWT_SECRET =
      "0123456789012345678901234567890123456789012345678901234567890123";
  private static final long TEST_ACCESS_TOKEN_TTL_SECONDS = 900;
  private static final long TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS = 86400;
  private static final long TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS = 1800;
  private static final long TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS = 3600;

  private AccessTokenCodec accessTokenCodec;

  @BeforeEach
  void setUp() {
    accessTokenCodec =
        new AccessTokenCodec(
            new AccessTokenSupport(TEST_JWT_SECRET),
            TEST_ACCESS_TOKEN_TTL_SECONDS,
            TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS);
  }

  @Test
  void extractUserIdReturnsSubjectForNormalAccessToken() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    Long userId =
        accessTokenCodec.extractUserId(
            accessTokenCodec.generateToken(user, AccessTokenType.ACCESS).token());

    assertEquals(42L, userId);
  }

  @Test
  void extractUserIdRejectsVerifyRegistrationToken() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    String verifyRegistrationToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL).token();

    assertThrows(
        AuthException.class, () -> accessTokenCodec.extractUserId(verifyRegistrationToken));
  }

  @Test
  void generateVerifyEmailTokenUsesTwentyFourHourTtl() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    AuthTokenDto verifyRegistrationToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL);

    assertEquals(TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS, verifyRegistrationToken.expiresIn());
  }

  @Test
  void generateVerifyLoginDeviceTokenUsesDedicatedTtl() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    AuthTokenDto verifyLoginDeviceToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE);

    assertEquals(TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS, verifyLoginDeviceToken.expiresIn());
  }

  @Test
  void generateForgotPasswordTokenUsesDedicatedTtl() {
    UserEntity user = EntityBuilder.buildUser("valid@tychewealth.com", "valid-user", "password");
    user.setId(42L);

    AuthTokenDto forgotPasswordToken =
        accessTokenCodec.generateToken(user, AccessTokenType.FORGOT_PASSWORD);

    assertEquals(TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS, forgotPasswordToken.expiresIn());
  }
}
