package com.tychewealth.service.helper.trusteddevice;

import com.tychewealth.entity.TrustedDeviceEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.TrustedDeviceRepository;
import com.tychewealth.utils.Utils;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TrustedDeviceHelper {

  private static final String TRUSTED_DEVICE_COOKIE_NAME = "trusted_device";
  private static final int MAX_TRUSTED_DEVICES_PER_USER = 3;
  private static final Duration TRUSTED_DEVICE_MAX_AGE = Duration.ofSeconds(Integer.MAX_VALUE);

  private final TrustedDeviceRepository trustedDeviceRepository;

  public ResponseCookie createTrustedDeviceCookie(UserEntity user) {
    validateTrustedDeviceLimit(user.getId());

    String trustedDeviceToken = UUID.randomUUID().toString();
    TrustedDeviceEntity trustedDevice = new TrustedDeviceEntity();
    trustedDevice.setUser(user);
    trustedDevice.setTokenHash(Utils.sha256Hex(trustedDeviceToken));
    trustedDevice.setExpiresAt(Instant.now().plus(TRUSTED_DEVICE_MAX_AGE));
    trustedDevice.setLastUsedAt(Instant.now());
    trustedDeviceRepository.save(trustedDevice);

    return buildTrustedDeviceCookie(trustedDeviceToken, TRUSTED_DEVICE_MAX_AGE);
  }

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
