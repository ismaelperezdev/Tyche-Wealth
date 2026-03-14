package com.tychewealth.dto.user.request;

import static com.tychewealth.constants.AuthConstants.BCRYPT_MAX_PASSWORD_BYTES;
import static com.tychewealth.constants.AuthConstants.LOGIN_PASSWORD_POLICY;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_LEAST_8_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED;
import static com.tychewealth.constants.ValidationConstants.MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordUpdateRequestDto {

  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 8, message = MUST_BE_AT_LEAST_8_CHARACTERS)
  private String currentPassword;

  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 8, message = MUST_BE_AT_LEAST_8_CHARACTERS)
  @Pattern(
      regexp = LOGIN_PASSWORD_POLICY,
      message = MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL)
  private String newPassword;

  @NotBlank(message = MUST_NOT_BE_BLANK)
  private String confirmNewPassword;

  @AssertTrue(message = MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED)
  private boolean isNewPasswordWithinBcryptLimit() {
    return newPassword == null
        || newPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_PASSWORD_BYTES;
  }

  @AssertTrue(message = "New password and confirm new password must match")
  private boolean isNewPasswordConfirmed() {
    return newPassword == null || newPassword.equals(confirmNewPassword);
  }
}
