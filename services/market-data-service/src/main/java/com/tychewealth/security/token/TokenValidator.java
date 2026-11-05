package com.tychewealth.security.token;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/** Validates bearer tokens issued by user-service for market-data API requests. */
@Component
public class TokenValidator {

  private final AccessTokenSupport accessTokenSupport;
  private final TokenStateSupport tokenStateSupport;

  public TokenValidator(
      AccessTokenSupport accessTokenSupport, TokenStateSupport tokenStateSupport) {
    this.accessTokenSupport = accessTokenSupport;
    this.tokenStateSupport = tokenStateSupport;
  }

  public Long validateAndExtractUserId(String authorizationHeader) {
    String token = tokenStateSupport.extractBearerToken(authorizationHeader);
    AccessTokenSupport.ParsedAccessToken parsedToken = accessTokenSupport.parseAccessToken(token);
    if (tokenStateSupport.isAccessTokenRevoked(parsedToken.tokenId())) {
      throw new BadCredentialsException("Access token is revoked or token state is unavailable");
    }
    return parsedToken.userId();
  }
}
