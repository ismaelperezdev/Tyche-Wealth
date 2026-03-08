package com.tychewealth.service;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;

public interface AuthService {

  UserResponseDto register(UserCreateRequestDto register);
}
