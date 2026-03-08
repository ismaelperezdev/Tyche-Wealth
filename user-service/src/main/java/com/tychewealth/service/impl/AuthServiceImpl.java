package com.tychewealth.service.impl;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.service.AuthService;
import com.tychewealth.service.helper.AuthRegisterHelper;
import com.tychewealth.service.helper.AuthValidationHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthValidationHelper authValidationHelper;
  private final AuthRegisterHelper authRegisterHelper;

  @Override
  public UserResponseDto register(UserCreateRequestDto register) {
    authValidationHelper.validateRegisterRequest(register);
    return authRegisterHelper.createUser(register);
  }
}
