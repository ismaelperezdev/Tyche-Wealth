package com.tychewealth.dto.user.request;

import com.tychewealth.utils.Utils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequestDto {

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
  private String username;

  public void setUsername(String username) {
    this.username = Utils.normalizeIdentity(username);
  }

  public UserUpdateRequestDto(String username) {
    setUsername(username);
  }
}
