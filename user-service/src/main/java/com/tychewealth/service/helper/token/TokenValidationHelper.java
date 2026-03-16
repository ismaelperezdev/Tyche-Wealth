package com.tychewealth.service.helper.token;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_AUTHORIZATION_HEADER_MESSAGE;
import static com.tychewealth.constants.LogConstants.INVALID_REFRESH_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REFRESH_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.monitoring.AuthMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidationHelper {

  private final AccessTokenHelper accessTokenHelper;
  private final AuthMetrics authMetrics;

  public Long validateAndExtractUserId(String authorizationHeader) {
    if (authorizationHeader == null
        || !authorizationHeader.regionMatches(
            true, 0, TOKEN_TYPE_BEARER_PREFIX, 0, TOKEN_TYPE_BEARER_PREFIX.length())) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_AUTHORIZATION_HEADER_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }

    String token = authorizationHeader.substring(TOKEN_TYPE_BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_AUTHORIZATION_HEADER_MESSAGE);
      throw new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
    }

    return accessTokenHelper.extractUserId(token);
  }

  public void validateRefreshTokenRequest(RefreshTokenRequestDto refreshTokenRequestDto) {
    if (refreshTokenRequestDto == null
        || !StringUtils.hasText(refreshTokenRequestDto.getRefreshToken())) {
      log.warn(REQUEST_CONFLICT, AUTH, REFRESH_TOKEN_ACTION, INVALID_REFRESH_TOKEN_MESSAGE);
      authMetrics.recordRefreshFailure();
      throw new AuthException(
          ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID, null, HttpStatus.UNAUTHORIZED);
    }
  }
}
