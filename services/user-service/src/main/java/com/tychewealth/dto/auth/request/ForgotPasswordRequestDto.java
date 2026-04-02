package com.tychewealth.dto.auth.request;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_A_VALID_EMAIL_ADDRESS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDto {

  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Email(message = MUST_BE_A_VALID_EMAIL_ADDRESS)
  private String email;
}
