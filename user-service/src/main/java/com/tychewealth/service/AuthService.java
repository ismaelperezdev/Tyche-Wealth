package com.tychewealth.service;

import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;

public interface AuthService {

  void verifyEmail(String token);

  UserResponseDto register(RegisterRequestDto register);

  void resendVerificationEmail(ResendVerificationEmailRequestDto resendVerificationEmailRequestDto);

  LoginResponseDto login(LoginRequestDto login);

  RefreshTokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);

  void logout(String authorizationHeader, RefreshTokenRequestDto refreshTokenRequestDto);
}
