package com.tychewealth.service.token;

import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_ACCESS_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.service.token.support.AccessTokenSupport;
import com.tychewealth.service.token.support.TokenStateSupport;
import com.tychewealth.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidator {

  private final AccessTokenSupport accessTokenSupport;
  private final TokenStateSupport tokenStateSupport;
  private final RedisTemplate<String, String> redisTemplate;

  public Long validateAndExtractUserId(String authorizationHeader) {
    String token = tokenStateSupport.extractBearerToken(authorizationHeader);
    String tokenId = accessTokenSupport.extractTokenId(token);
    if (tokenStateSupport.isAccessTokenRevoked(redisTemplate, tokenId)) {
      log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
      throw Utils.unauthorized();
    }

    return accessTokenSupport.extractUserId(token);
  }
}
