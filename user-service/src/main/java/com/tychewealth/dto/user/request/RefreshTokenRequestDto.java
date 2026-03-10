package com.tychewealth.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {

  @NotBlank(message = "Refresh token cannot be blank")
  private String refreshToken;
}
