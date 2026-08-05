package com.tychewealth.service;

import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import org.springframework.http.ResponseCookie;

/**
 * Application service contract for authentication and credential lifecycle operations.
 *
 * <p>Coordinates registration, login, email and device verification, password-recovery delivery,
 * refresh-token rotation, and logout without exposing the underlying persistence or token-state
 * implementation to API adapters.
 */
public interface AuthService {

  ResponseCookie verifyEmail(String token);

  ResponseCookie verifyLoginDevice(String token);

  void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequestDto);

  UserResponseDto register(RegisterRequestDto register);

  void resendVerificationEmail(ResendVerificationEmailRequestDto resendVerificationEmailRequestDto);

  LoginResponseDto login(LoginRequestDto login);

  RefreshTokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);

  void logout(String authorizationHeader, RefreshTokenRequestDto refreshTokenRequestDto);
}
