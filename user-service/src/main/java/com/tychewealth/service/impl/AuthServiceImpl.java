package com.tychewealth.service.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.AuthService;
import com.tychewealth.service.EmailService;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.email.RegisterEmailHelper;
import com.tychewealth.service.helper.token.AccessTokenHelper;
import com.tychewealth.service.helper.token.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.token.TokenStateHelper;
import com.tychewealth.service.helper.token.TokenValidationHelper;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.token.AuthTokenPayload;
import com.tychewealth.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthValidationHelper authValidationHelper;
  private final AuthRegisterHelper authRegisterHelper;
  private final AuthLoginHelper authLoginHelper;
  private final RegisterEmailHelper registerEmailHelper;
  private final EmailService emailService;
  private final TokenStateHelper tokenStateHelper;
  private final AuthRefreshTokenHelper authRefreshTokenHelper;
  private final AccessTokenHelper accessTokenHelper;
  private final TokenValidationHelper tokenValidationHelper;
  private final AuthMetrics authMetrics;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void verifyEmail(String token) {
    if (token == null || token.isBlank()) {
      throw new AuthException(ErrorDefinition.GENERIC_BAD_REQUEST, null, HttpStatus.BAD_REQUEST);
    }

    Long userId = accessTokenHelper.extractVerifyEmailUserId(token);
    UserEntity user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () ->
                    new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED));
    if (user.isVerified()) {
      return;
    }

    user.setVerified(true);
    user.setVerificationTokenExpiresAt(null);
    userRepository.save(user);
  }

  @Override
  @Transactional
  public UserResponseDto register(RegisterRequestDto register) {
    authValidationHelper.validateRegisterRequest(register);

    try {
      var registeredUser = authRegisterHelper.createUser(register);
      scheduleVerificationEmail(
          registeredUser.response().getEmail(), registeredUser.verificationToken());
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              authMetrics.recordRegisterSuccess();
              log.info(
                  LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
                  LogConstants.AUTH,
                  LogConstants.REGISTER_ACTION,
                  registeredUser.response().getId());
            }
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
    UserEntity user =
        userRepository
            .findByEmailAndDeletedAtIsNull(
                Utils.normalizeIdentity(resendVerificationEmailRequestDto.getEmail()))
            .orElseThrow(
                () ->
                    new AuthException(ErrorDefinition.USER_NOT_FOUND, null, HttpStatus.NOT_FOUND));
    authValidationHelper.validateVerificationEmailCanBeResent(user);
    AuthTokenPayload verificationToken = accessTokenHelper.generateVerifyEmailToken(user);
    user.setVerificationTokenExpiresAt(
        accessTokenHelper.extractExpiration(verificationToken.accessToken()));
    scheduleVerificationEmail(user.getEmail(), verificationToken);
  }

  @Override
  public LoginResponseDto login(LoginRequestDto login) {
    UserEntity user = authValidationHelper.validateLoginRequest(login);
    return authLoginHelper.login(user);
  }

  @Override
  @Transactional
  public RefreshTokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto) {
    tokenValidationHelper.validateRefreshTokenRequest(refreshTokenRequestDto);

    RefreshTokenEntity currentRefreshToken =
        authRefreshTokenHelper.validateRefreshToken(refreshTokenRequestDto.getRefreshToken());

    UserEntity user = currentRefreshToken.getUser();
    AuthTokenPayload accessTokenPayload = accessTokenHelper.generateAccessToken(user);

    tokenStateHelper.unlinkRefreshToken(refreshTokenRequestDto.getRefreshToken());
    AuthRefreshTokenHelper.LinkedRefreshToken newRefreshToken =
        authRefreshTokenHelper.saveToken(
            user, accessTokenPayload.jti(), LogConstants.REFRESH_TOKEN_ACTION);
    authMetrics.recordRefreshSuccess();

    return new RefreshTokenResponseDto(
        accessTokenPayload.tokenType(),
        accessTokenPayload.accessToken(),
        accessTokenPayload.expiresIn(),
        newRefreshToken.token());
  }

  @Override
  @Transactional
  public void logout(String authorizationHeader, RefreshTokenRequestDto refreshTokenRequestDto) {
    tokenValidationHelper.validateRefreshTokenRequest(refreshTokenRequestDto);
    String refreshTokenValue = refreshTokenRequestDto.getRefreshToken();
    RefreshTokenEntity refreshToken =
        authRefreshTokenHelper.validateRefreshToken(refreshTokenValue);
    tokenStateHelper.revokeAccessTokenIfPresent(authorizationHeader);
    tokenStateHelper
        .findAccessTokenJtiByRefreshToken(refreshTokenValue)
        .ifPresent(
            tokenId -> tokenStateHelper.revokeAccessToken(tokenId, refreshToken.getExpiresAt()));
    tokenStateHelper.unlinkRefreshToken(refreshTokenValue);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGOUT_ACTION,
        refreshToken.getUser().getId());
  }

  private void scheduleVerificationEmail(String email, AuthTokenPayload verificationToken) {
    var verificationEmailMessage =
        registerEmailHelper.buildVerifyEmailMessage(
            email, verificationToken.accessToken(), verificationToken.expiresIn());

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            emailService.send(verificationEmailMessage);
          }
        });
  }
}
