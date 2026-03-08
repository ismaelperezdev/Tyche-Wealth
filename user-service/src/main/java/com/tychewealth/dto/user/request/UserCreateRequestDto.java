package com.tychewealth.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequestDto {

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Email format is invalid")
  @Size(max = 254, message = "Email must be at most 254 characters")
  private String email;

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
  private String username;

  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
  private String password;
}
