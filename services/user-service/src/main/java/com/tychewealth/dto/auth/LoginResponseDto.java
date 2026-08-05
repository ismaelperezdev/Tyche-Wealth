package com.tychewealth.dto.auth;

import com.tychewealth.dto.user.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after successful user authentication.
 *
 * <p>Provides the access and refresh tokens, the access-token lifetime, and the public user
 * representation needed by the client to establish the authenticated session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

  private String tokenType;
  private String accessToken;
  private String refreshToken;
  private long expiresIn;
  private UserResponseDto user;
}
