package com.tychewealth.service.helper.auth;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.constants.RedisConstants;
import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.enums.EmailSendResult;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.exception.EmailException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.RateLimitStore;
import com.tychewealth.service.email.AuthEmailFactory;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Completes the authenticated login flow after credentials have been validated.
 *
 * <p>Checks trusted-device status, optionally triggers device verification by email, creates a new
 * access token, revokes previous refresh tokens, persists the rotated refresh token, and returns
 * the public user data with the newly issued credentials.
 */
@Slf4j
@Component
@AllArgsConstructor
public class AuthLoginHelper {

  private static final Duration LOGIN_DEVICE_EMAIL_COOLDOWN = Duration.ofDays(1);

  private final AccessTokenCodec accessTokenCodec;
  private final AuthRefreshTokenHelper refreshTokenHelper;
  private final TrustedDeviceManager trustedDeviceManager;
  private final AuthEmailFactory authEmailFactory;
  private final EmailSender emailSender;
  private final RateLimitStore rateLimitStore;
  private final UserMapper userMapper;
  private final AuthMetrics authMetrics;

  public LoginResponseDto login(UserEntity user) {

    handleUntrustedDeviceLogin(user);

    UserResponseDto response = userMapper.toDto(user);
    AuthTokenDto tokenPayload = accessTokenCodec.generateToken(user, AccessTokenType.ACCESS);
    refreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    AuthRefreshTokenHelper.LinkedRefreshToken refreshToken =
        refreshTokenHelper.saveToken(user, tokenPayload.jti(), LogConstants.LOGIN_ACTION);
    authMetrics.incrementMetric(AuthMetricEnum.LOGIN_SUCCESS);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        user.getId());

    return new LoginResponseDto(
        tokenPayload.tokenType(),
        tokenPayload.token(),
        refreshToken.token(),
        tokenPayload.expiresIn(),
        response);
  }

  private void handleUntrustedDeviceLogin(UserEntity user) {
    if (trustedDeviceManager.isTrustedCurrentDevice(user)) {
      return;
    }

    if (trustedDeviceManager.hasReachedTrustedDeviceLimit(user.getId())) {
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
    String userId = String.valueOf(user.getId());
    if (!canSendLoginVerificationEmail(user)) {
      log.info(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          "login verification email cooldown active",
          user.getId());
      return;
    }

    try {
      AuthTokenDto verificationToken =
          accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE);
      var loginDeviceEmailMessage =
          authEmailFactory.buildVerifyLoginDeviceEmailMessage(
              user.getEmail(), verificationToken.token(), verificationToken.expiresIn());
      EmailSendResult sendResult = emailSender.send(loginDeviceEmailMessage);
      if (sendResult == EmailSendResult.SKIPPED_DAILY_QUOTA) {
        throw EmailException.of(
            ErrorDefinition.EMAIL_DELIVERY_FAILED, null, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (RuntimeException ex) {
      rollbackLoginVerificationEmailCooldown(userId, user.getId(), ex);
      throw ex;
    }
    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
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

  private void rollbackLoginVerificationEmailCooldown(
      String userId, Long logUserId, RuntimeException emailSendException) {
    try {
      rateLimitStore.clear(RedisConstants.AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE, userId);
    } catch (RuntimeException rollbackException) {
      rollbackException.addSuppressed(emailSendException);
      log.warn(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          "failed to roll back login verification email cooldown after send failure",
          logUserId,
          rollbackException);
      return;
    }

    log.warn(
        LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        "rolled back login verification email cooldown after send failure",
        logUserId,
        emailSendException);
  }
}
