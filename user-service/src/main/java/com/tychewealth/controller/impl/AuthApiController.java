package com.tychewealth.controller.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.AuthApi;
import com.tychewealth.dto.user.LoginResponseDto;
import com.tychewealth.dto.user.RefreshTokenResponseDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.LoginRequestDto;
import com.tychewealth.dto.user.request.RefreshTokenRequestDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
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
  public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto register) {
    log.info(
        LogConstants.REQUEST_START + LogConstants.REGISTER_REQUEST_FIELDS,
        LogConstants.AUTH,
        LogConstants.REGISTER_ACTION,
        LogContextFactory.mask(register.getUsername()),
        LogContextFactory.mask(register.getEmail()));

    UserResponseDto response = authService.register(register);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto login) {
    log.info(
        LogConstants.REQUEST_START + LogConstants.LOGIN_REQUEST_FIELDS,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        LogContextFactory.mask(login.getEmail()));

    LoginResponseDto response = authService.login(login);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<RefreshTokenResponseDto> refresh(
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
    log.info(LogConstants.REQUEST_START, LogConstants.AUTH, LogConstants.REFRESH_TOKEN_ACTION);

    RefreshTokenResponseDto response = authService.refresh(refreshTokenRequestDto);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
