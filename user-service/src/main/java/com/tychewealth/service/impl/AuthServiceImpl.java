package com.tychewealth.service.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.AuthService;
import com.tychewealth.service.email.VerificationEmailWorkflow;
import com.tychewealth.service.helper.auth.AuthForgotPasswordHelper;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthResendVerificationEmailHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.auth.AuthVerifyEmailHelper;
import com.tychewealth.service.helper.auth.AuthVerifyLoginDeviceHelper;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.token.TokenStateStore;
import com.tychewealth.service.token.TokenValidator;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthValidationHelper authValidationHelper;
  private final AuthRegisterHelper authRegisterHelper;
  private final AuthLoginHelper authLoginHelper;
  private final AuthResendVerificationEmailHelper authResendVerificationEmailHelper;
  private final AuthVerifyEmailHelper authVerifyEmailHelper;
  private final AuthVerifyLoginDeviceHelper authVerifyLoginDeviceHelper;
  private final VerificationEmailWorkflow verificationEmailWorkflow;
  private final TokenStateStore tokenStateStore;
  private final AuthRefreshTokenHelper authRefreshTokenHelper;
  private final AccessTokenCodec accessTokenCodec;
  private final TokenValidator tokenValidator;
  private final AuthForgotPasswordHelper authForgotPasswordHelper;
  private final AuthMetrics authMetrics;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public ResponseCookie verifyEmail(String token) {
    return authVerifyEmailHelper.verify(token);
  }

  @Override
  @Transactional
  public ResponseCookie verifyLoginDevice(String token) {
    return authVerifyLoginDeviceHelper.verify(token);
  }

  @Override
  public void forgotPassword(ForgotPasswordRequestDto requestDto) {
    authForgotPasswordHelper.forgotPassword(requestDto);
  }

  @Override
  @Transactional
  public UserResponseDto register(RegisterRequestDto register) {
    authValidationHelper.validateRegisterRequest(register);

    try {
      var registeredUser = authRegisterHelper.createUser(register);
      Instant failedAttemptExpiry =
          accessTokenCodec.extractExpiration(registeredUser.verificationToken().token());

      verificationEmailWorkflow.scheduleVerificationEmail(
          registeredUser.response().getId(),
          registeredUser.response().getEmail(),
          registeredUser.verificationToken(),
          failedAttemptExpiry,
          null,
          () -> {
            authMetrics.incrementMetric(AuthMetricEnum.REGISTER_SUCCESS);
            log.info(
                LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
                LogConstants.AUTH,
                LogConstants.REGISTER_ACTION,
                registeredUser.response().getId());
          });

      return registeredUser.response();
    } catch (DataIntegrityViolationException ex) {
      throw authValidationHelper.validateRegisterPersistenceConflict(ex);
    }
  }

  @Override
  @Transactional
  public void resendVerificationEmail(
      ResendVerificationEmailRequestDto resendVerificationEmailRequestDto) {
    authResendVerificationEmailHelper.resendVerificationEmail(resendVerificationEmailRequestDto);
  }

  @Override
  public LoginResponseDto login(LoginRequestDto login) {
    UserEntity user = authValidationHelper.validateLoginRequest(login);
    return authLoginHelper.login(user);
  }

  @Override
  @Transactional
  public RefreshTokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto) {
    tokenValidator.validateRefreshTokenRequest(refreshTokenRequestDto);

    RefreshTokenEntity currentRefreshToken =
        authRefreshTokenHelper.validateRefreshToken(refreshTokenRequestDto.getRefreshToken());

    UserEntity user = currentRefreshToken.getUser();
    AuthTokenDto accessTokenPayload = accessTokenCodec.generateToken(user, AccessTokenType.ACCESS);

    tokenStateStore.unlinkRefreshToken(refreshTokenRequestDto.getRefreshToken());
    AuthRefreshTokenHelper.LinkedRefreshToken newRefreshToken =
        authRefreshTokenHelper.saveToken(
            user, accessTokenPayload.jti(), LogConstants.REFRESH_TOKEN_ACTION);
    authMetrics.incrementMetric(AuthMetricEnum.REFRESH_SUCCESS);

    return new RefreshTokenResponseDto(
        accessTokenPayload.tokenType(),
        accessTokenPayload.token(),
        accessTokenPayload.expiresIn(),
        newRefreshToken.token());
  }

  @Override
  @Transactional
  public void logout(String authorizationHeader, RefreshTokenRequestDto refreshTokenRequestDto) {
    tokenValidator.validateRefreshTokenRequest(refreshTokenRequestDto);
    String refreshTokenValue = refreshTokenRequestDto.getRefreshToken();
    RefreshTokenEntity refreshToken =
        authRefreshTokenHelper.validateRefreshToken(refreshTokenValue);
    tokenStateStore.revokeAccessTokenIfPresent(authorizationHeader);
    tokenStateStore
        .findAccessTokenJtiByRefreshToken(refreshTokenValue)
        .ifPresent(
            tokenId -> tokenStateStore.revokeAccessToken(tokenId, refreshToken.getExpiresAt()));
    tokenStateStore.unlinkRefreshToken(refreshTokenValue);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGOUT_ACTION,
        refreshToken.getUser().getId());
  }
}
