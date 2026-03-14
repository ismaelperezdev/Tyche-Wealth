package com.tychewealth.dto.auth.request;

import static com.tychewealth.constants.AuthConstants.BCRYPT_MAX_PASSWORD_BYTES;
import static com.tychewealth.constants.AuthConstants.LOGIN_PASSWORD_POLICY;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_LEAST_8_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_254_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_A_VALID_EMAIL_ADDRESS;
import static com.tychewealth.constants.ValidationConstants.MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import com.tychewealth.utils.Utils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Email(message = MUST_BE_A_VALID_EMAIL_ADDRESS)
  @Size(max = 254, message = MUST_BE_AT_MOST_254_CHARACTERS)
  private String email;

  @Setter
  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 8, message = MUST_BE_AT_LEAST_8_CHARACTERS)
  @Pattern(
      regexp = LOGIN_PASSWORD_POLICY,
      message = MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL)
  private String password;

  public void setEmail(String email) {
    this.email = Utils.normalizeIdentity(email);
  }

  @AssertTrue(message = MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED)
  private boolean isPasswordWithinBcryptLimit() {
    return password == null
        || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_PASSWORD_BYTES;
  }
}
