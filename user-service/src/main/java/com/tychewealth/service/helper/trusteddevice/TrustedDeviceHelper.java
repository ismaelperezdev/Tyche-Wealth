package com.tychewealth.service.helper.trusteddevice;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.TrustedDeviceRepository;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TrustedDeviceHelper {

  private static final String TRUSTED_DEVICE_COOKIE_NAME = "trusted_device";
  private static final int MAX_TRUSTED_DEVICES_PER_USER = 3;

  private final TrustedDeviceRepository trustedDeviceRepository;

  public ResponseCookie buildTrustedDeviceCookie(String trustedDeviceToken, Duration maxAge) {
    return ResponseCookie.from(TRUSTED_DEVICE_COOKIE_NAME, trustedDeviceToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(maxAge)
        .build();
  }

  public void validateTrustedDeviceLimit(Long userId) {
    if (trustedDeviceRepository.countByUserId(userId) >= MAX_TRUSTED_DEVICES_PER_USER) {
      throw new AuthException(ErrorDefinition.CONFLICT, null, HttpStatus.CONFLICT);
    }
  }
}
