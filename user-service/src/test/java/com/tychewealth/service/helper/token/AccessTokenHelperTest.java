package com.tychewealth.service.helper.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.testdata.EntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessTokenHelperTest {

  private static final String TEST_JWT_SECRET =
      "0123456789012345678901234567890123456789012345678901234567890123";

  private AccessTokenHelper accessTokenHelper;

  @BeforeEach
  void setUp() {
    accessTokenHelper = new AccessTokenHelper(TEST_JWT_SECRET, 900, 900);
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
}
