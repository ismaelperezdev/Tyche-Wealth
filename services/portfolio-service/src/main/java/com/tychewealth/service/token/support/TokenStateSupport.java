package com.tychewealth.service.token.support;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_AUTHORIZATION_HEADER_MESSAGE;
import static com.tychewealth.constants.LogConstants.REDIS_UNAVAILABLE_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenStateSupport {

  private static final String ACCESS_TOKEN_REVOCATION_KEY_PREFIX =
      "auth:access-token:blacklist:jti:";

  public boolean isAccessTokenRevoked(RedisTemplate<String, String> redisTemplate, String tokenId) {
    try {
      return redisTemplate.hasKey(buildAccessTokenRevocationKey(tokenId));
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + " tokenId={}",
          AUTH,
          ACCESS_TOKEN_ACTION,
          REDIS_UNAVAILABLE_MESSAGE,
          tokenId,
          ex);
      return true;
    }
  }

  public String extractBearerToken(String authorizationHeader) {
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

    return token;
  }

  private String buildAccessTokenRevocationKey(String tokenId) {
    return ACCESS_TOKEN_REVOCATION_KEY_PREFIX + tokenId;
  }
}
