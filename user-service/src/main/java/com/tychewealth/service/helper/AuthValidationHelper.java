package com.tychewealth.service.helper;

import static com.tychewealth.constants.AuthConstants.LOGIN_PASSWORD_POLICY;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.request.LoginRequestDto;
import com.tychewealth.dto.user.request.RefreshTokenRequestDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.utils.Utils;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@AllArgsConstructor
public class AuthValidationHelper {

  private static final Pattern LOGIN_PASSWORD_PATTERN = Pattern.compile(LOGIN_PASSWORD_POLICY);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthMetrics authMetrics;

  public void validateRegisterRequest(RegisterRequestDto register) {
    validateEmailIsAvailable(register.getEmail());
    validateUsernameIsAvailable(register.getUsername());
    validateRegisterPasswordFormat(register.getPassword());
  }

  public UserEntity validateLoginRequest(LoginRequestDto login) {
    UserEntity user = validateLoginEmail(login.getEmail());
    validateLoginPassword(login.getPassword(), user.getPassword());
    return user;
  }

  public void validateEmailIsAvailable(String email) {
    String normalizedEmail = Utils.normalizeIdentity(email);
    if (userRepository.findByEmail(normalizedEmail).isPresent()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "email already exists");
      authMetrics.recordRegisterFailure();
      authMetrics.recordRegisterConflict();

      throw new AuthException(
          ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
    }
  }

  public void validateUsernameIsAvailable(String username) {
    String normalizedUsername = Utils.normalizeIdentity(username);
    if (userRepository.findByUsername(normalizedUsername).isPresent()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "username already exists");
      authMetrics.recordRegisterFailure();
      authMetrics.recordRegisterConflict();

      throw new AuthException(
          ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
    }
  }

  public UserEntity validateLoginEmail(String email) {
    String normalizedEmail = Utils.normalizeIdentity(email);
    return userRepository
        .findByEmail(normalizedEmail)
        .orElseThrow(
            () -> {
              log.warn(
                  LogConstants.REQUEST_CONFLICT,
                  LogConstants.AUTH,
                  LogConstants.LOGIN_ACTION,
                  "invalid login credentials");
              authMetrics.recordLoginFailure();
              authMetrics.recordLoginInvalidCredentials();
              return new AuthException(
                  ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, null, HttpStatus.UNAUTHORIZED);
            });
  }

  public void validateLoginPassword(String rawPassword, String encodedPassword) {
    validateLoginPasswordFormat(rawPassword);
    validateLoginPasswordMatches(rawPassword, encodedPassword);
  }

  public void validateRegisterPasswordFormat(String password) {
    if (password == null || !LOGIN_PASSWORD_PATTERN.matcher(password).matches()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "invalid password format for register");
      authMetrics.recordRegisterFailure();

      throw new AuthException(
          ErrorDefinition.AUTH_REGISTER_PASSWORD_FORMAT_INVALID, null, HttpStatus.BAD_REQUEST);
    }
  }

  public void validateLoginPasswordFormat(String password) {
    if (password == null || !LOGIN_PASSWORD_PATTERN.matcher(password).matches()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          "invalid password format for login");
      authMetrics.recordLoginFailure();

      throw new AuthException(
          ErrorDefinition.AUTH_LOGIN_PASSWORD_FORMAT_INVALID, null, HttpStatus.BAD_REQUEST);
    }
  }

  public void validateLoginPasswordMatches(String rawPassword, String encodedPassword) {
    if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.LOGIN_ACTION,
          "invalid login credentials");
      authMetrics.recordLoginFailure();
      authMetrics.recordLoginInvalidCredentials();

      throw new AuthException(
          ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, null, HttpStatus.UNAUTHORIZED);
    }
  }

  public void validateRefreshTokenRequest(RefreshTokenRequestDto refreshTokenRequestDto) {
    if (refreshTokenRequestDto == null
        || !StringUtils.hasText(refreshTokenRequestDto.getRefreshToken())) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REFRESH_TOKEN_ACTION,
          LogConstants.INVALID_REFRESH_TOKEN_MESSAGE);
      authMetrics.recordRefreshFailure();

      throw new AuthException(
          ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID, null, HttpStatus.UNAUTHORIZED);
    }
  }
}
