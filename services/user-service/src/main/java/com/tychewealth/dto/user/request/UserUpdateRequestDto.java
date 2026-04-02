package com.tychewealth.dto.user.request;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_30_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

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

  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 3, max = 30, message = MUST_BE_BETWEEN_3_AND_30_CHARACTERS)
  private String username;

  public void setUsername(String username) {
    this.username = Utils.normalizeIdentity(username);
  }

  public UserUpdateRequestDto(String username) {
    setUsername(username);
  }
}
