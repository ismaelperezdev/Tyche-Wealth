package com.tychewealth.service.helper.auth;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.constants.RedisConstants;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.service.EmailService;
import com.tychewealth.service.helper.email.LoginDeviceEmailHelper;
import com.tychewealth.service.helper.token.AccessTokenHelper;
import com.tychewealth.service.helper.token.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.trusteddevice.TrustedDeviceHelper;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.ratelimit.RateLimitStore;
import com.tychewealth.service.token.AuthTokenPayload;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthLoginHelper {

  private static final Duration LOGIN_DEVICE_EMAIL_COOLDOWN = Duration.ofDays(1);

  private final AccessTokenHelper accessTokenHelper;
  private final AuthRefreshTokenHelper refreshTokenHelper;
  private final TrustedDeviceHelper trustedDeviceHelper;
  private final LoginDeviceEmailHelper loginDeviceEmailHelper;
  private final EmailService emailService;
  private final RateLimitStore rateLimitStore;
  private final UserMapper userMapper;
  private final AuthMetrics authMetrics;

  public LoginResponseDto login(UserEntity user) {

    handleUntrustedDeviceLogin(user);

    UserResponseDto response = userMapper.toDto(user);
    AuthTokenPayload tokenPayload = accessTokenHelper.generateAccessToken(user);
    refreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    AuthRefreshTokenHelper.LinkedRefreshToken refreshToken =
        refreshTokenHelper.saveToken(user, tokenPayload.jti(), LogConstants.LOGIN_ACTION);
    authMetrics.recordLoginSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        user.getId());

    return new LoginResponseDto(
        tokenPayload.tokenType(),
        tokenPayload.accessToken(),
        refreshToken.token(),
        tokenPayload.expiresIn(),
        response);
  }

  private void handleUntrustedDeviceLogin(UserEntity user) {
    if (trustedDeviceHelper.isTrustedCurrentDevice(user)) {
      return;
    }

    if (trustedDeviceHelper.hasReachedTrustedDeviceLimit(user.getId())) {
      log.warn(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          "trusted device limit reached for unrecognized device",
          user.getId());
      throw AuthException.of(
          ErrorDefinition.AUTH_TRUSTED_DEVICE_LIMIT_REACHED, null, HttpStatus.CONFLICT);
    }

    sendLoginVerificationEmailIfAllowed(user);
    throw AuthException.of(
        ErrorDefinition.AUTH_LOGIN_DEVICE_VERIFICATION_REQUIRED, null, HttpStatus.FORBIDDEN);
  }

  private void sendLoginVerificationEmailIfAllowed(UserEntity user) {
    if (!canSendLoginVerificationEmail(user)) {
      log.info(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          "login verification email cooldown active",
          user.getId());
      return;
    }

    AuthTokenPayload verificationToken = accessTokenHelper.generateVerifyLoginDeviceToken(user);
    var loginDeviceEmailMessage =
        loginDeviceEmailHelper.buildVerifyLoginDeviceEmailMessage(
            user.getEmail(), verificationToken.accessToken(), verificationToken.expiresIn());

    emailService.send(loginDeviceEmailMessage);
    log.info(
        LogConstants.REQUEST_START + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        user.getId());
  }

  private boolean canSendLoginVerificationEmail(UserEntity user) {
    try {
      long attempts =
          rateLimitStore.increment(
              RedisConstants.AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
              String.valueOf(user.getId()),
              LOGIN_DEVICE_EMAIL_COOLDOWN);
      return attempts == 1;
    } catch (RuntimeException ex) {
      log.warn(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE,
          user.getId(),
          ex);
      return true;
    }
  }
}
