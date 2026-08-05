package com.tychewealth.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public representation of a user account returned by the user-service API.
 *
 * <p>Contains the user's identity and creation metadata while intentionally excluding sensitive
 * persistence fields such as password hashes, verification state, and token expiration data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {

  private Long id;
  private String email;
  private String username;
  private LocalDateTime createdAt;
}
