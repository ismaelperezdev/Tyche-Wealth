package com.tychewealth.service;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;

public interface UserService {

  UserResponseDto retrieve(String authorizationHeader);

  UserResponseDto update(String authorizationHeader, UserUpdateRequestDto updateRequest);

  Long updatePassword(
      String authorizationHeader, UserPasswordUpdateRequestDto updatePasswordRequest);

  Long delete(String authorizationHeader);
}
