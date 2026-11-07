package com.tychewealth.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Validates user-service access-token claims and their HS256 signature. */
@Component
public class AccessTokenSupport {

  private final SecretKey signingKey;

  public AccessTokenSupport(@Value("${app.auth.jwt.secret}") String jwtSecret) {
    signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public ParsedAccessToken parseAccessToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

      if (StringUtils.hasText(claims.get("purpose", String.class))) {
        throw new BadCredentialsException("Token is not an access token");
      }

      if (claims.getExpiration() == null) {
        throw new BadCredentialsException("Access token has no expiration");
      }

      String tokenId = claims.getId();
      if (!StringUtils.hasText(tokenId)) {
        throw new BadCredentialsException("Access token has no identifier");
      }

      String subject = claims.getSubject();
      if (!StringUtils.hasText(subject)) {
        throw new BadCredentialsException("Access token has no subject");
      }

      return new ParsedAccessToken(Long.valueOf(subject), tokenId);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new BadCredentialsException("Invalid access token", exception);
    }
  }

  public record ParsedAccessToken(Long userId, String tokenId) {}
}
