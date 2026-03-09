package com.tychewealth.service.helper;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.LoginResponseDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.service.token.AuthTokenPayload;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthLoginHelper {

  private final AuthTokenHelper authTokenHelper;
  private final UserMapper userMapper;

  public LoginResponseDto login(UserEntity user) {
    UserResponseDto response = userMapper.toDto(user);
    AuthTokenPayload tokenPayload = authTokenHelper.generateAccessToken(user);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.LOGIN_USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        user.getId());

    return new LoginResponseDto(
        tokenPayload.tokenType(), tokenPayload.accessToken(), tokenPayload.expiresIn(), response);
  }
}
