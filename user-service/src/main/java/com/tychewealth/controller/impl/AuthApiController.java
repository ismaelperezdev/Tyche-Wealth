package com.tychewealth.controller.impl;

import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.LOGIN_ACTION;
import static com.tychewealth.constants.LogConstants.LOGIN_REQUEST_FIELDS;
import static com.tychewealth.constants.LogConstants.LOGOUT_ACTION;
import static com.tychewealth.constants.LogConstants.REFRESH_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.REGISTER_ACTION;
import static com.tychewealth.constants.LogConstants.REGISTER_REQUEST_FIELDS;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.VERIFY_EMAIL_ACTION;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.utils.LogContextFactory.mask;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.http.ResponseEntity.status;

import com.tychewealth.controller.AuthApi;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
public class AuthApiController implements AuthApi {

  private final AuthService authService;

  @Override
  public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
    log.info(REQUEST_START, AUTH, VERIFY_EMAIL_ACTION);

    authService.verifyEmail(token);
    return ResponseEntity.noContent()
        .header(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .build();
  }

  @Override
  public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto register) {
    log.info(
        REQUEST_START + REGISTER_REQUEST_FIELDS,
        AUTH,
        REGISTER_ACTION,
        mask(register.getUsername()),
        mask(register.getEmail()));

    UserResponseDto response = authService.register(register);
    return withNoStoreHeaders(status(HttpStatus.CREATED)).body(response);
  }

  @Override
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto login) {
    log.info(REQUEST_START + LOGIN_REQUEST_FIELDS, AUTH, LOGIN_ACTION, mask(login.getEmail()));

    LoginResponseDto response = authService.login(login);
    return withNoStoreHeaders(status(HttpStatus.OK)).body(response);
  }

  @Override
  public ResponseEntity<RefreshTokenResponseDto> refresh(
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
    log.info(REQUEST_START, AUTH, REFRESH_TOKEN_ACTION);

    RefreshTokenResponseDto response = authService.refresh(refreshTokenRequestDto);
    return withNoStoreHeaders(status(HttpStatus.OK)).body(response);
  }

  @Override
  public ResponseEntity<Void> logout(
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
    log.info(REQUEST_START, AUTH, LOGOUT_ACTION);

    authService.logout(authorizationHeader, refreshTokenRequestDto);
    return ResponseEntity.noContent()
        .header(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .build();
  }

  private ResponseEntity.BodyBuilder withNoStoreHeaders(ResponseEntity.BodyBuilder builder) {
    return builder
        .header(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE);
  }
}
