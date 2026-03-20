package com.tychewealth.controller.impl;

import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.AuthApi;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.service.AuthService;
import com.tychewealth.utils.LogContextFactory;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    return withNoStoreHeaders(ResponseEntity.status(HttpStatus.CREATED)).body(response);
  }

  @Override
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto login) {
    log.info(
        LogConstants.REQUEST_START + LogConstants.LOGIN_REQUEST_FIELDS,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        LogContextFactory.mask(login.getEmail()));

    LoginResponseDto response = authService.login(login);
    return withNoStoreHeaders(ResponseEntity.status(HttpStatus.OK)).body(response);
  }

  @Override
  public ResponseEntity<RefreshTokenResponseDto> refresh(
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
    log.info(LogConstants.REQUEST_START, LogConstants.AUTH, LogConstants.REFRESH_TOKEN_ACTION);

    RefreshTokenResponseDto response = authService.refresh(refreshTokenRequestDto);
    return withNoStoreHeaders(ResponseEntity.status(HttpStatus.OK)).body(response);
  }

  @Override
  public ResponseEntity<Void> logout(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
    log.info(LogConstants.REQUEST_START, LogConstants.AUTH, LogConstants.LOGOUT_ACTION);

    authService.logout(refreshTokenRequestDto);
    return ResponseEntity.noContent()
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .build();
  }

  private ResponseEntity.BodyBuilder withNoStoreHeaders(ResponseEntity.BodyBuilder builder) {
    return builder
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE);
  }
}
