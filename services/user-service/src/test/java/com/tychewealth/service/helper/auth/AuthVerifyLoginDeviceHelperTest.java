package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_COOKIE_NAME;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_LOGIN_DEVICE_TOKEN;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;

@ExtendWith(MockitoExtension.class)
class AuthVerifyLoginDeviceHelperTest {

  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private UserRepository userRepository;
  @Mock private TrustedDeviceManager trustedDeviceManager;

  @InjectMocks private AuthVerifyLoginDeviceHelper authVerifyLoginDeviceHelper;

  @Test
  void verifyThrowsBadRequestWhenTokenIsBlank() {
    AuthException exception =
        assertThrows(AuthException.class, () -> authVerifyLoginDeviceHelper.verify(" "));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void verifyThrowsUnauthorizedWhenUserDoesNotExist() {
    when(accessTokenCodec.extractVerifyLoginDeviceUserId(TEST_VERIFY_LOGIN_DEVICE_TOKEN))
        .thenReturn(TEST_USER_ID);
    when(userRepository.findByIdAndDeletedAtIsNull(TEST_USER_ID)).thenReturn(Optional.empty());

    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authVerifyLoginDeviceHelper.verify(TEST_VERIFY_LOGIN_DEVICE_TOKEN));

    assertEquals(ErrorDefinition.UNAUTHORIZED, exception.getErrorDefinition());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
  }

  @Test
  void verifyReturnsTrustedDeviceCookieWhenUserExists() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    ResponseCookie cookie =
        ResponseCookie.from(TEST_TRUSTED_DEVICE_COOKIE_NAME, TEST_TRUSTED_DEVICE_TOKEN)
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ofHours(1))
            .build();

    when(accessTokenCodec.extractVerifyLoginDeviceUserId(TEST_VERIFY_LOGIN_DEVICE_TOKEN))
        .thenReturn(TEST_USER_ID);
    when(userRepository.findByIdAndDeletedAtIsNull(TEST_USER_ID)).thenReturn(Optional.of(user));
    when(trustedDeviceManager.createTrustedDeviceCookie(user)).thenReturn(cookie);

    ResponseCookie result = authVerifyLoginDeviceHelper.verify(TEST_VERIFY_LOGIN_DEVICE_TOKEN);

    assertSame(cookie, result);
    verify(trustedDeviceManager).createTrustedDeviceCookie(user);
  }
}
