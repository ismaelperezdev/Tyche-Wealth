package com.tychewealth.service;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;

/**
 * Application service contract for authenticated user-account operations.
 *
 * <p>Defines the use cases for retrieving and updating the current user, changing the password, and
 * performing a soft account deletion while keeping validation, persistence, and token-state
 * handling behind the service boundary.
 */
public interface UserService {

  UserResponseDto retrieve(Long userId);

  UserResponseDto update(Long userId, UserUpdateRequestDto updateRequest);

  Long updatePassword(
      Long userId, String authorizationHeader, UserPasswordUpdateRequestDto updatePasswordRequest);

  Long delete(Long userId, String authorizationHeader);
}
