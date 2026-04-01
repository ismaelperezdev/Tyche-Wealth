package com.tychewealth.service.token.support;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.testdata.EntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessTokenSupportTest {

  private AccessTokenSupport accessTokenSupport;
  private AccessTokenCodec accessTokenCodec;

  @BeforeEach
  void setUp() {
    accessTokenSupport = new AccessTokenSupport(TEST_JWT_SECRET);
    accessTokenCodec =
        new AccessTokenCodec(
            accessTokenSupport,
            TEST_ACCESS_TOKEN_TTL_SECONDS,
            TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS);
  }

  @Test
  void requirePositiveTtlRejectsNonPositiveValues() {
    assertThrows(
        IllegalArgumentException.class,
        () -> accessTokenSupport.requirePositiveTtl(0, "app.auth.jwt.access-token-ttl-seconds"));
    assertThrows(
        IllegalArgumentException.class,
        () -> accessTokenSupport.requirePositiveTtl(-1, "app.auth.jwt.access-token-ttl-seconds"));
  }

  @Test
  void extractVerifyEmailUserIdReturnsSubjectForVerifyEmailToken() {
    UserEntity user = buildUser();

    String token = accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL).token();

    assertEquals(TEST_USER_ID, accessTokenSupport.extractVerifyEmailUserId(token));
  }

  @Test
  void extractVerifyLoginDeviceUserIdReturnsSubjectForVerifyLoginDeviceToken() {
    UserEntity user = buildUser();

    String token =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE).token();

    assertEquals(TEST_USER_ID, accessTokenSupport.extractVerifyLoginDeviceUserId(token));
  }

  @Test
  void extractTokenIdReturnsJwtId() {
    UserEntity user = buildUser();

    String token = accessTokenCodec.generateToken(user, AccessTokenType.ACCESS).token();

    assertNotNull(accessTokenSupport.extractTokenId(token));
  }

  @Test
  void extractUserIdRejectsPurposeBoundTokens() {
    UserEntity user = buildUser();

    String token = accessTokenCodec.generateToken(user, AccessTokenType.FORGOT_PASSWORD).token();

    assertThrows(AuthException.class, () -> accessTokenSupport.extractUserId(token));
  }

  private UserEntity buildUser() {
    UserEntity user =
        EntityBuilder.buildUser(TEST_EMAIL_VALID, TEST_USERNAME_VALID, TEST_PASSWORD_VALID);
    user.setId(TEST_USER_ID);
    return user;
  }
}
