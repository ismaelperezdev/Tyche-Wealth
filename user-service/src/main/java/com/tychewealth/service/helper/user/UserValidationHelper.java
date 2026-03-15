package com.tychewealth.service.helper.user;

import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.monitoring.UserMetrics;
import com.tychewealth.utils.Utils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidationHelper {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMetrics userMetrics;

  public void validateUsernameIsAvailableForUpdate(String username, Long currentUserId) {
    String normalizedUsername = Utils.normalizeIdentity(username);
    userRepository
        .findByUsernameAndDeletedAtIsNull(normalizedUsername)
        .filter(user -> !user.getId().equals(currentUserId))
        .ifPresent(
            user -> {
              userMetrics.recordUsernameConflict();
              throw new UserException(
                  ErrorDefinition.USER_USERNAME_CONFLICT, null, HttpStatus.CONFLICT);
            });
  }

  public void validateCurrentPassword(String rawPassword, String encodedPassword) {
    if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
      userMetrics.recordCurrentPasswordInvalid();
      throw new UserException(
          ErrorDefinition.USER_CURRENT_PASSWORD_INVALID, null, HttpStatus.UNAUTHORIZED);
    }
  }

  public void validateNewPasswordIsDifferent(String newRawPassword, String encodedPassword) {
    if (passwordEncoder.matches(newRawPassword, encodedPassword)) {
      userMetrics.recordNewPasswordReused();
      throw new UserException(
          ErrorDefinition.USER_NEW_PASSWORD_MUST_BE_DIFFERENT, null, HttpStatus.BAD_REQUEST);
    }
  }
}
