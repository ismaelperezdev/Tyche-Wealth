package com.tychewealth.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserUpdateRequestDto {

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
  private String username;
}
