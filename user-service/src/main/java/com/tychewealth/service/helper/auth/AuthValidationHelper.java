package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.EMAIL_CONSTRAINT;
import static com.tychewealth.constants.AuthConstants.LOGIN_PASSWORD_POLICY;
import static com.tychewealth.constants.AuthConstants.USERNAME_CONSTRAINT;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.utils.Utils;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    validateUserIsVerified(user);
    validateLoginPassword(login.getPassword(), user.getPassword());
    return user;
  }

  public void validateUserIsVerified(UserEntity user) {
    if (user.isVerified()) {
      return;
    }

    log.warn(
        LogConstants.REQUEST_CONFLICT,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        LogConstants.INVALID_LOGIN_CREDENTIALS_MESSAGE);
    authMetrics.incrementMetric(AuthMetricEnum.LOGIN_FAILURE);
    authMetrics.incrementMetric(AuthMetricEnum.LOGIN_INVALID_CREDENTIALS);
    throw new AuthException(
        ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, null, HttpStatus.UNAUTHORIZED);
  }

  public boolean canResendVerificationEmail(UserEntity user) {

    if (user.isVerified()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.VERIFY_REGISTRATION_ACTION,
          "user is already verified");
      return false;
    }

    Instant verificationTokenExpiresAt = user.getVerificationTokenExpiresAt();
    if (verificationTokenExpiresAt == null || !verificationTokenExpiresAt.isAfter(Instant.now())) {
      return true;
    }

    log.warn(
        LogConstants.REQUEST_CONFLICT,
        LogConstants.AUTH,
        LogConstants.VERIFY_REGISTRATION_ACTION,
        "previous verification email is still available");
    return false;
  }

  public AuthException validateRegisterPersistenceConflict(DataIntegrityViolationException ex) {
    if (!validateUserUniqueConstraintViolation(ex)) {
      throw ex;
    }

    log.warn(
        LogConstants.REQUEST_CONFLICT,
        LogConstants.AUTH,
        LogConstants.REGISTER_ACTION,
        "registration conflict detected at persistence layer");
    authMetrics.incrementMetric(AuthMetricEnum.REGISTER_FAILURE);
    authMetrics.incrementMetric(AuthMetricEnum.REGISTER_CONFLICT);

    return new AuthException(ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
  }

  public void validateEmailIsAvailable(String email) {
    String normalizedEmail = Utils.normalizeIdentity(email);
    if (userRepository.findByEmailIncludingDeleted(normalizedEmail).isPresent()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "email already exists");
      authMetrics.incrementMetric(AuthMetricEnum.REGISTER_FAILURE);
      authMetrics.incrementMetric(AuthMetricEnum.REGISTER_CONFLICT);

      throw new AuthException(
          ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
    }
  }

  public void validateUsernameIsAvailable(String username) {
    String normalizedUsername = Utils.normalizeIdentity(username);
    if (userRepository.findByUsernameIncludingDeleted(normalizedUsername).isPresent()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "username already exists");
      authMetrics.incrementMetric(AuthMetricEnum.REGISTER_FAILURE);
      authMetrics.incrementMetric(AuthMetricEnum.REGISTER_CONFLICT);

      throw new AuthException(
          ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
    }
  }

  public UserEntity validateLoginEmail(String email) {
    String normalizedEmail = Utils.normalizeIdentity(email);
    return userRepository
        .findByEmailAndDeletedAtIsNull(normalizedEmail)
        .orElseThrow(
            () -> {
              log.warn(
                  LogConstants.REQUEST_CONFLICT,
                  LogConstants.AUTH,
                  LogConstants.LOGIN_ACTION,
                  LogConstants.INVALID_LOGIN_CREDENTIALS_MESSAGE);
              authMetrics.incrementMetric(AuthMetricEnum.LOGIN_FAILURE);
              authMetrics.incrementMetric(AuthMetricEnum.LOGIN_INVALID_CREDENTIALS);
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
          LogConstants.INVALID_PASSWORD_FORMAT_MESSAGE);
      authMetrics.incrementMetric(AuthMetricEnum.REGISTER_FAILURE);

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
          LogConstants.INVALID_PASSWORD_FORMAT_MESSAGE);
      authMetrics.incrementMetric(AuthMetricEnum.LOGIN_FAILURE);

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
          LogConstants.INVALID_LOGIN_CREDENTIALS_MESSAGE);
      authMetrics.incrementMetric(AuthMetricEnum.LOGIN_FAILURE);
      authMetrics.incrementMetric(AuthMetricEnum.LOGIN_INVALID_CREDENTIALS);

      throw new AuthException(
          ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, null, HttpStatus.UNAUTHORIZED);
    }
  }

  private boolean validateUserUniqueConstraintViolation(Throwable throwable) {
    Throwable current = throwable;

    while (current != null) {
      if (current instanceof ConstraintViolationException cve) {
        String constraintName = cve.getConstraintName();
        if (validateUserUniqueConstraint(constraintName)) {
          return true;
        }
      }

      String message = current.getMessage();
      if (validateUserUniqueConstraint(message)) {
        return true;
      }

      current = current.getCause();
    }
    return false;
  }

  private boolean validateUserUniqueConstraint(String source) {
    if (source == null || source.isBlank()) {
      return false;
    }
    String normalized = source.toLowerCase(Locale.ROOT);
    return normalized.contains(EMAIL_CONSTRAINT) || normalized.contains(USERNAME_CONSTRAINT);
  }
}
