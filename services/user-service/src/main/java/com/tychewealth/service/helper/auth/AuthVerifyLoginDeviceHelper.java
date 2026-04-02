package com.tychewealth.service.helper.auth;

import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthVerifyLoginDeviceHelper {

  private final AccessTokenCodec accessTokenCodec;
  private final UserRepository userRepository;
  private final TrustedDeviceManager trustedDeviceManager;

  public ResponseCookie verify(String token) {
    if (token == null || token.isBlank()) {
      throw new AuthException(ErrorDefinition.GENERIC_BAD_REQUEST, null, HttpStatus.BAD_REQUEST);
    }

    Long userId = accessTokenCodec.extractVerifyLoginDeviceUserId(token);
    UserEntity user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () ->
                    new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED));

    return trustedDeviceManager.createTrustedDeviceCookie(user);
  }
}
