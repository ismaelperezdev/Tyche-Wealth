package com.tychewealth.service.helper;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.LoginResponseDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.token.AuthTokenPayload;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthLoginHelper {

  private final AuthTokenHelper authTokenHelper;
  private final AuthRefreshTokenHelper refreshTokenHelper;
  private final UserMapper userMapper;
  private final AuthMetrics authMetrics;

  public LoginResponseDto login(UserEntity user) {
    UserResponseDto response = userMapper.toDto(user);
    AuthTokenPayload tokenPayload = authTokenHelper.generateAccessToken(user);
    refreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    String refreshToken = refreshTokenHelper.generateRefreshToken();
    Instant refreshTokenExpiresAt = refreshTokenHelper.calculateRefreshTokenExpiration();
    refreshTokenHelper.saveToken(user, refreshToken, refreshTokenExpiresAt);
    authMetrics.recordLoginSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.LOGIN_USER_ID,
        LogConstants.AUTH,
        LogConstants.LOGIN_ACTION,
        user.getId());

    return new LoginResponseDto(
        tokenPayload.tokenType(),
        tokenPayload.accessToken(),
        refreshToken,
        tokenPayload.expiresIn(),
        response);
  }
}
