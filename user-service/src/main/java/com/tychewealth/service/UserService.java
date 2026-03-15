package com.tychewealth.service;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;

public interface UserService {

  UserResponseDto retrieve(Long userId);

  UserResponseDto update(Long userId, UserUpdateRequestDto updateRequest);

  Long updatePassword(Long userId, UserPasswordUpdateRequestDto updatePasswordRequest);

  Long delete(Long userId);
}
