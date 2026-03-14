package com.tychewealth.service.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import org.junit.jupiter.api.Test;

class AuthTokenHelperTest {

  private static final String JWT_SECRET = "4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=";

  private final AuthTokenHelper authTokenHelper = new AuthTokenHelper(JWT_SECRET, 900);

  @Test
  void extractUserIdAcceptsLowercaseBearerScheme() {
    UserEntity user = new UserEntity();
    user.setId(7L);
    user.setEmail("test@email.com");
    user.setUsername("test-user");

    String accessToken = authTokenHelper.generateAccessToken(user).accessToken();

    Long userId = authTokenHelper.extractUserId("bearer " + accessToken);

    assertEquals(7L, userId);
  }

  @Test
  void extractUserIdRejectsInvalidAuthorizationScheme() {
    assertThrows(AuthException.class, () -> authTokenHelper.extractUserId("basic token"));
  }
}
