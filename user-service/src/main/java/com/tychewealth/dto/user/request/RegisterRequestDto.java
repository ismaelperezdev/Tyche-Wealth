package com.tychewealth.dto.user.request;

import static com.tychewealth.constants.AuthConstants.BCRYPT_MAX_PASSWORD_BYTES;

import com.tychewealth.utils.Utils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Email format is invalid")
  @Size(max = 254, message = "Email must be at most 254 characters")
  private String email;

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
  private String username;

  @Setter
  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;

  public void setEmail(String email) {
    this.email = Utils.normalizeIdentity(email);
  }

  public void setUsername(String username) {
    this.username = Utils.normalizeIdentity(username);
  }

  @AssertTrue(message = "Password must be at most 72 bytes when UTF-8 encoded")
  private boolean isPasswordWithinBcryptLimit() {
    return password == null
        || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_PASSWORD_BYTES;
  }
}
