package com.tychewealth.dto.auth;

import com.tychewealth.dto.user.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
