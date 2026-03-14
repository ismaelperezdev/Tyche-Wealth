package com.tychewealth.service.helper;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.monitoring.AuthMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@AllArgsConstructor
public class TokenValidationHelper {

  private final AuthMetrics authMetrics;

  public void validateRefreshTokenRequest(RefreshTokenRequestDto refreshTokenRequestDto) {
    if (refreshTokenRequestDto == null
        || !StringUtils.hasText(refreshTokenRequestDto.getRefreshToken())) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.AUTH,
          LogConstants.REFRESH_TOKEN_ACTION,
          LogConstants.INVALID_REFRESH_TOKEN_MESSAGE);
      authMetrics.recordRefreshFailure();

      throw new AuthException(
          ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID, null, HttpStatus.UNAUTHORIZED);
    }
  }
}
