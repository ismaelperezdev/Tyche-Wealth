package com.tychewealth.dto.user;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponseDto {

  private String tokenType;
  private String accessToken;
  private Instant expiresAt;
  private String refreshToken;
}
