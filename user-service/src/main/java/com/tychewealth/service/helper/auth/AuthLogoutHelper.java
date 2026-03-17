package com.tychewealth.service.helper.auth;

import com.tychewealth.service.helper.token.AccessTokenHelper;
import com.tychewealth.service.helper.token.AccessTokenRevocationHelper;
import com.tychewealth.service.helper.token.TokenValidationHelper;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthLogoutHelper {

  private final TokenValidationHelper tokenValidationHelper;
  private final AccessTokenHelper accessTokenHelper;
  private final AccessTokenRevocationHelper accessTokenRevocationHelper;

  public void revokeAccessTokenIfPresent(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return;
    }

    String accessToken = tokenValidationHelper.extractBearerToken(authorizationHeader);
    Instant expiresAt = accessTokenHelper.extractExpiration(accessToken);
    accessTokenRevocationHelper.revoke(accessToken, expiresAt);
  }
}
