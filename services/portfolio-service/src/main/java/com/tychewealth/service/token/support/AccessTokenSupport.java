package com.tychewealth.service.token.support;

import static com.tychewealth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.INVALID_ACCESS_TOKEN_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.utils.Utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AccessTokenSupport {

  private final SecretKey signingKey;

  public AccessTokenSupport(@Value("${app.auth.jwt.secret}") String jwtSecret) {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public Claims parseValidatedClaims(String token) {
    try {
      JwtParser jwtParser = Jwts.parser().verifyWith(signingKey).build();
      return jwtParser.parseSignedClaims(token).getPayload();
    } catch (JwtException | IllegalArgumentException ex) {
      throw unauthorizedException();
    }
  }

  public Long extractUserId(String token) {
    Claims claims = parseValidatedClaims(token);
    String purpose = claims.get("purpose", String.class);
    if (StringUtils.hasText(purpose)) {
      throw unauthorizedException();
    }

    try {
      return Long.valueOf(claims.getSubject());
    } catch (IllegalArgumentException ex) {
      throw unauthorizedException();
    }
  }

  public String extractTokenId(String token) {
    String tokenId = parseValidatedClaims(token).getId();
    if (!StringUtils.hasText(tokenId)) {
      throw unauthorizedException();
    }
    return tokenId;
  }

  private RuntimeException unauthorizedException() {
    log.warn(REQUEST_CONFLICT, AUTH, ACCESS_TOKEN_ACTION, INVALID_ACCESS_TOKEN_MESSAGE);
    return Utils.unauthorized();
  }
}
