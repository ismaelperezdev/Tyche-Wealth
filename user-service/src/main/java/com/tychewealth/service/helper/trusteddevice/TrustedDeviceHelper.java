package com.tychewealth.service.helper.trusteddevice;

import com.tychewealth.entity.TrustedDeviceEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.TrustedDeviceRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.utils.Utils;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@AllArgsConstructor
public class TrustedDeviceHelper {

  private static final String TRUSTED_DEVICE_COOKIE_NAME = "trusted_device";
  private static final int MAX_TRUSTED_DEVICES_PER_USER = 3;
  private static final Duration TRUSTED_DEVICE_MAX_AGE = Duration.ofSeconds(Integer.MAX_VALUE);

  private final TrustedDeviceRepository trustedDeviceRepository;
  private final UserRepository userRepository;

  @Transactional
  public ResponseCookie createTrustedDeviceCookie(UserEntity user) {
    Instant now = Instant.now();
    String trustedDeviceToken = extractTrustedDeviceToken().orElse(null);

    if (StringUtils.hasText(trustedDeviceToken)) {
      Optional<TrustedDeviceEntity> existingTrustedDevice =
          trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
              user.getId(), Utils.sha256Hex(trustedDeviceToken), now);

      if (existingTrustedDevice.isPresent()) {
        TrustedDeviceEntity trustedDevice = existingTrustedDevice.get();
        trustedDevice.setExpiresAt(now.plus(TRUSTED_DEVICE_MAX_AGE));
        trustedDevice.setLastUsedAt(now);
        trustedDeviceRepository.save(trustedDevice);
        return buildTrustedDeviceCookie(trustedDeviceToken, TRUSTED_DEVICE_MAX_AGE);
      }
    }

    userRepository
        .findByIdForUpdate(user.getId())
        .orElseThrow(
            () -> new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED));
    validateTrustedDeviceLimit(user.getId(), now);

    trustedDeviceToken = UUID.randomUUID().toString();
    TrustedDeviceEntity trustedDevice = new TrustedDeviceEntity();
    trustedDevice.setUser(user);
    trustedDevice.setTokenHash(Utils.sha256Hex(trustedDeviceToken));
    trustedDevice.setExpiresAt(now.plus(TRUSTED_DEVICE_MAX_AGE));
    trustedDevice.setLastUsedAt(now);
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

  public void validateTrustedDeviceLimit(Long userId, Instant now) {
    trustedDeviceRepository.deleteByUserIdAndExpiresAtBefore(userId, now);

    if (trustedDeviceRepository.countByUserIdAndExpiresAtAfter(userId, now)
        >= MAX_TRUSTED_DEVICES_PER_USER) {
      throw new AuthException(ErrorDefinition.CONFLICT, null, HttpStatus.CONFLICT);
    }
  }

  public boolean hasReachedTrustedDeviceLimit(Long userId) {
    Instant now = Instant.now();
    trustedDeviceRepository.deleteByUserIdAndExpiresAtBefore(userId, now);
    return trustedDeviceRepository.countByUserIdAndExpiresAtAfter(userId, now)
        >= MAX_TRUSTED_DEVICES_PER_USER;
  }

  public boolean isTrustedCurrentDevice(UserEntity user) {
    Instant now = Instant.now();
    return extractTrustedDeviceToken()
        .map(Utils::sha256Hex)
        .flatMap(
            tokenHash ->
                trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
                    user.getId(), tokenHash, now))
        .isPresent();
  }

  private Optional<String> extractTrustedDeviceToken() {
    ServletRequestAttributes requestAttributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return Optional.empty();
    }

    Cookie[] cookies = requestAttributes.getRequest().getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    return Arrays.stream(cookies)
        .filter(cookie -> TRUSTED_DEVICE_COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> value != null && !value.isBlank())
        .findFirst();
  }
}
