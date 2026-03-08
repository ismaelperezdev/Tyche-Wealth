package com.tychewealth.service.helper;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthValidationHelper {

  private final UserRepository userRepository;

  public void validateRegisterRequest(UserCreateRequestDto register) {

    if (userRepository.findByEmail(register.getEmail()).isPresent()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "email already exists");

      throw new AuthException(
          ErrorDefinition.AUTH_EMAIL_ALREADY_EXISTS_ERROR,
          Map.of("email", register.getEmail()),
          HttpStatus.CONFLICT);
    }

    if (userRepository.findByUsername(register.getUsername()).isPresent()) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REGISTER_ACTION,
          "username already exists");

      throw new AuthException(
          ErrorDefinition.AUTH_USERNAME_ALREADY_EXISTS_ERROR,
          Map.of("username", register.getUsername()),
          HttpStatus.CONFLICT);
    }
  }
}
