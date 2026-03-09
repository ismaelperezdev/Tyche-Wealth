package com.tychewealth.dto.user;

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
  private long expiresIn;
  private UserResponseDto user;
}
