package com.tychewealth.controller.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.AuthApi;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.service.AuthService;
import com.tychewealth.utils.LogContextFactory;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
public class AuthApiController implements AuthApi {

  private final AuthService authService;

  @Override
  public ResponseEntity<UserResponseDto> register(
      @Valid @RequestBody UserCreateRequestDto register) {
    log.info(
        LogConstants.REQUEST_START + LogConstants.REGISTER_REQUEST_FIELDS,
        LogConstants.AUTH,
        LogConstants.REGISTER_ACTION,
        LogContextFactory.mask(register.getUsername()),
        LogContextFactory.mask(register.getEmail()));

    UserResponseDto response = authService.register(register);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
