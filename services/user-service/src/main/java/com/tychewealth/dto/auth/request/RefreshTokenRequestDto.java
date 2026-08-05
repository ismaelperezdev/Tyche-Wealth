package com.tychewealth.dto.auth.request;

import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload containing the refresh token used to obtain a new access-token pair. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {

  @NotBlank(message = MUST_NOT_BE_BLANK)
  private String refreshToken;
}
