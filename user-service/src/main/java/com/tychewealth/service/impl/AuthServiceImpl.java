package com.tychewealth.service.impl;

import static com.tychewealth.constants.AuthConstants.EMAIL_CONSTRAINT;
import static com.tychewealth.constants.AuthConstants.USERNAME_CONSTRAINT;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.AuthService;
import com.tychewealth.service.helper.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.AuthTokenHelper;
import com.tychewealth.service.helper.TokenValidationHelper;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.token.AuthTokenPayload;
import java.time.Instant;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthValidationHelper authValidationHelper;
  private final AuthRegisterHelper authRegisterHelper;
  private final AuthLoginHelper authLoginHelper;
  private final AuthRefreshTokenHelper authRefreshTokenHelper;
  private final AuthTokenHelper authTokenHelper;
  private final TokenValidationHelper tokenValidationHelper;
  private final AuthMetrics authMetrics;

  @Override
  public UserResponseDto register(RegisterRequestDto register) {
    authValidationHelper.validateRegisterRequest(register);

    try {
      return authRegisterHelper.createUser(register);
    } catch (DataIntegrityViolationException ex) {
      if (!isUserUniqueConstraintViolation(ex)) {
        throw ex;
      }

      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "registration conflict detected at persistence layer");
      authMetrics.recordRegisterFailure();
      authMetrics.recordRegisterConflict();

      throw new AuthException(
          ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
    }
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
    AuthTokenPayload accessTokenPayload = authTokenHelper.generateAccessToken(user);

    String newRefreshToken = authRefreshTokenHelper.generateRefreshToken();
    Instant newRefreshTokenExpiration = authRefreshTokenHelper.calculateRefreshTokenExpiration();
    authRefreshTokenHelper.saveToken(user, newRefreshToken, newRefreshTokenExpiration);
    authMetrics.recordRefreshSuccess();

    return new RefreshTokenResponseDto(
        accessTokenPayload.tokenType(),
        accessTokenPayload.accessToken(),
        accessTokenPayload.expiresIn(),
        newRefreshToken);
  }

  @Override
  @Transactional
  public void logout(RefreshTokenRequestDto refreshTokenRequestDto) {
    tokenValidationHelper.validateRefreshTokenRequest(refreshTokenRequestDto);
    RefreshTokenEntity refreshToken =
        authRefreshTokenHelper.validateRefreshToken(refreshTokenRequestDto.getRefreshToken());

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGOUT_ACTION,
        refreshToken.getUser().getId());
  }

  private boolean isUserUniqueConstraintViolation(Throwable throwable) {
    Throwable current = throwable;

    while (current != null) {
      if (current instanceof ConstraintViolationException cve) {
        String constraintName = cve.getConstraintName();
        if (isUserUniqueConstraint(constraintName)) {
          return true;
        }
      }

      String message = current.getMessage();
      if (isUserUniqueConstraint(message)) {
        return true;
      }

      current = current.getCause();
    }
    return false;
  }

  private boolean isUserUniqueConstraint(String source) {
    if (source == null || source.isBlank()) {
      return false;
    }
    String normalized = source.toLowerCase(Locale.ROOT);
    return normalized.contains(EMAIL_CONSTRAINT) || normalized.contains(USERNAME_CONSTRAINT);
  }
}
