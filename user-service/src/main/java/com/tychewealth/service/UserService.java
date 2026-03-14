package com.tychewealth.service;

import com.tychewealth.dto.user.UserResponseDto;

public interface UserService {

  UserResponseDto retrieve(String authorizationHeader);

  Long delete(String authorizationHeader);
}
