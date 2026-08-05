package com.tychewealth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after a refresh-token operation succeeds.
 *
 * <p>Contains the newly issued access token, its type and lifetime, and the rotated refresh token
 * used by the client for the next renewal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponseDto {

  private String tokenType;
  private String accessToken;
  private long expiresIn;
  private String refreshToken;
}
