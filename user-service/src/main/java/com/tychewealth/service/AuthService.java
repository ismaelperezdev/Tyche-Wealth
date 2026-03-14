package com.tychewealth.service;

import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;

public interface AuthService {

  UserResponseDto register(RegisterRequestDto register);

  LoginResponseDto login(LoginRequestDto login);

  RefreshTokenResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);

  void logout(RefreshTokenRequestDto refreshTokenRequestDto);
}
