package com.tychewealth.service.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.AuthService;
import com.tychewealth.service.helper.AuthRegisterHelper;
import com.tychewealth.service.helper.AuthValidationHelper;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthValidationHelper authValidationHelper;
  private final AuthRegisterHelper authRegisterHelper;

  @Override
  public UserResponseDto register(UserCreateRequestDto register) {
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
      throw new AuthException(
          ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);
    }
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
    return normalized.contains("uk_users_email") || normalized.contains("uk_users_username");
  }
}
