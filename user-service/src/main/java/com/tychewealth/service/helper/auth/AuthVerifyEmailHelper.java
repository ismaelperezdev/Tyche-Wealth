package com.tychewealth.service.helper.auth;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthVerifyEmailHelper {

  private final AccessTokenCodec accessTokenCodec;
  private final UserRepository userRepository;
  private final TrustedDeviceManager trustedDeviceManager;

  public ResponseCookie verify(String token) {
    if (token == null || token.isBlank()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.VERIFY_REGISTRATION_ACTION,
          "missing verification token");
      throw new AuthException(ErrorDefinition.GENERIC_BAD_REQUEST, null, HttpStatus.BAD_REQUEST);
    }

    Long userId = accessTokenCodec.extractVerifyEmailUserId(token);
    UserEntity user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () ->
                    new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED));

    if (user.isVerified()) {
      log.info(
          LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
          LogConstants.AUTH,
          LogConstants.VERIFY_REGISTRATION_ACTION,
          user.getId());
      return trustedDeviceManager.createTrustedDeviceCookie(user);
    }

    user.setVerified(true);
    user.setVerificationTokenExpiresAt(null);
    userRepository.save(user);
    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.VERIFY_REGISTRATION_ACTION,
        user.getId());

    return trustedDeviceManager.createTrustedDeviceCookie(user);
  }
}
