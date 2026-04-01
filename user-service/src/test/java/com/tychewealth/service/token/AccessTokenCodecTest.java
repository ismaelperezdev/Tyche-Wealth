package com.tychewealth.service.token;

import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_VALID;
import static com.tychewealth.constants.TestConstants.TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_JWT_SECRET;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.service.token.support.AccessTokenSupport;
import com.tychewealth.testdata.EntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessTokenCodecTest {

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
    UserEntity user = buildUser();

    Long userId =
        accessTokenCodec.extractUserId(
            accessTokenCodec.generateToken(user, AccessTokenType.ACCESS).token());

    assertEquals(42L, userId);
  }

  @Test
  void extractUserIdRejectsVerifyRegistrationToken() {
    UserEntity user = buildUser();

    String verifyRegistrationToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL).token();

    assertThrows(
        AuthException.class, () -> accessTokenCodec.extractUserId(verifyRegistrationToken));
  }

  @Test
  void generateVerifyEmailTokenUsesTwentyFourHourTtl() {
    UserEntity user = buildUser();

    AuthTokenDto verifyRegistrationToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL);

    assertEquals(TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS, verifyRegistrationToken.expiresIn());
  }

  @Test
  void generateVerifyLoginDeviceTokenUsesDedicatedTtl() {
    UserEntity user = buildUser();

    AuthTokenDto verifyLoginDeviceToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE);

    assertEquals(TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS, verifyLoginDeviceToken.expiresIn());
  }

  @Test
  void generateForgotPasswordTokenUsesDedicatedTtl() {
    UserEntity user = buildUser();

    AuthTokenDto forgotPasswordToken =
        accessTokenCodec.generateToken(user, AccessTokenType.FORGOT_PASSWORD);

    assertEquals(TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS, forgotPasswordToken.expiresIn());
  }

  @Test
  void generateAccessTokenIncludesTokenId() {
    UserEntity user = buildUser();

    AuthTokenDto accessToken = accessTokenCodec.generateToken(user, AccessTokenType.ACCESS);

    assertTrue(accessToken.jti() != null && !accessToken.jti().isBlank());
    assertEquals(accessToken.jti(), accessTokenCodec.extractTokenId(accessToken.token()));
  }

  private UserEntity buildUser() {
    UserEntity user =
        EntityBuilder.buildUser(TEST_EMAIL_VALID, TEST_USERNAME_VALID, TEST_PASSWORD_VALID);
    user.setId(TEST_USER_ID);
    return user;
  }
}
