package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.REQUEST_CONSUMES;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;
import static com.tychewealth.constants.ApiConstants.URL_FOLDER_V1;

import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(value = URL_FOLDER_V1 + "/auth")
@Tag(name = "Auth")
public interface AuthApi {

  @GetMapping(value = "/verify-registration", produces = REQUEST_PRODUCES)
  ResponseEntity<Void> verifyRegistration(@RequestParam("token") String token);

  @GetMapping(value = "/verify-login-device", produces = REQUEST_PRODUCES)
  ResponseEntity<Void> verifyLoginDevice(@RequestParam("token") String token);

  @GetMapping(value = "/forgot-password", produces = REQUEST_PRODUCES)
  ResponseEntity<Void> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequestDto forgotPasswordRequestDto);

  @PostMapping(value = "/register", consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto register);

  @PostMapping(value = "/login", consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto login);

  @PostMapping(value = "/resend-verification", consumes = REQUEST_CONSUMES)
  ResponseEntity<Void> resendVerificationEmail(
      @Valid @RequestBody ResendVerificationEmailRequestDto resendVerificationEmailRequestDto);

  @PostMapping(value = "/refresh", consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<RefreshTokenResponseDto> refresh(
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto);

  @PostMapping(value = "/logout", consumes = REQUEST_CONSUMES)
  ResponseEntity<Void> logout(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto);
}
