package com.tychewealth.service.helper;

import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.utils.Utils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidationHelper {

  private final UserRepository userRepository;

  public void validateUsernameIsAvailableForUpdate(String username, Long currentUserId) {
    String normalizedUsername = Utils.normalizeIdentity(username);
    userRepository
        .findByUsername(normalizedUsername)
        .filter(user -> !user.getId().equals(currentUserId))
        .ifPresent(
            user -> {
              throw new UserException(
                  ErrorDefinition.USER_USERNAME_CONFLICT, null, HttpStatus.CONFLICT);
            });
  }
}
