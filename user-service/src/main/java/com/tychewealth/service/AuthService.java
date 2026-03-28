package com.tychewealth.service;

import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import org.springframework.http.ResponseCookie;

public interface AuthService {

  ResponseCookie verifyEmail(String token);

  ResponseCookie verifyLoginDevice(String token);

  void forgotPassword(ResendVerificationEmailRequestDto resendVerificationEmailRequestDto);

  UserResponseDto register(RegisterRequestDto register);

  void resendVerificationEmail(ResendVerificationEmailRequestDto resendVerificationEmailRequestDto);

  LoginResponseDto login(LoginRequestDto login);

  RefreshTokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);

  void logout(String authorizationHeader, RefreshTokenRequestDto refreshTokenRequestDto);
}
