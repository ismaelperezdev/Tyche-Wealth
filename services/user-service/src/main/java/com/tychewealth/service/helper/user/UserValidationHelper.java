package com.tychewealth.service.helper.user;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.UserMetricEnum;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
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
              log.warn(
                  LogConstants.REQUEST_CONFLICT,
                  LogConstants.USER,
                  LogConstants.UPDATE_ACTION,
                  "username conflict");
              userMetrics.incrementMetric(UserMetricEnum.USERNAME_CONFLICT);
              throw new UserException(
                  ErrorDefinition.USER_USERNAME_CONFLICT, null, HttpStatus.CONFLICT);
            });
  }

  public void validateCurrentPassword(String rawPassword, String encodedPassword) {
    if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.USER,
          LogConstants.UPDATE_PASSWORD_ACTION,
          "invalid current password");
      userMetrics.incrementMetric(UserMetricEnum.CURRENT_PASSWORD_INVALID);
      throw new UserException(
          ErrorDefinition.USER_CURRENT_PASSWORD_INVALID, null, HttpStatus.UNAUTHORIZED);
    }
  }

  public void validateNewPasswordIsDifferent(String newRawPassword, String encodedPassword) {
    if (passwordEncoder.matches(newRawPassword, encodedPassword)) {
      log.warn(
          LogConstants.REQUEST_CONFLICT,
          LogConstants.USER,
          LogConstants.UPDATE_PASSWORD_ACTION,
          "new password reused");
      userMetrics.incrementMetric(UserMetricEnum.NEW_PASSWORD_REUSED);
      throw new UserException(
          ErrorDefinition.USER_NEW_PASSWORD_MUST_BE_DIFFERENT, null, HttpStatus.BAD_REQUEST);
    }
  }

  public void validatePasswordUpdate(
      UserPasswordUpdateRequestDto updatePasswordRequest, UserEntity user) {
    validateCurrentPassword(updatePasswordRequest.getCurrentPassword(), user.getPassword());
    validateNewPasswordIsDifferent(updatePasswordRequest.getNewPassword(), user.getPassword());
  }
}
