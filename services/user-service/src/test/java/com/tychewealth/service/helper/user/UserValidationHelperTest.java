package com.tychewealth.service.helper.user;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_ENCODED_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.MetricsTestHelper.counterValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.UserMetricEnum;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserValidationHelperTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private SimpleMeterRegistry meterRegistry;
  private UserValidationHelper userValidationHelper;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    userValidationHelper =
        new UserValidationHelper(userRepository, passwordEncoder, new UserMetrics(meterRegistry));
  }

  @Test
  void validateUsernameIsAvailableForUpdatePassesWhenUsernameBelongsToCurrentUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);

    when(userRepository.findByUsernameAndDeletedAtIsNull(TEST_USERNAME_LAURA))
        .thenReturn(Optional.of(user));

    userValidationHelper.validateUsernameIsAvailableForUpdate(TEST_USERNAME_LAURA, TEST_USER_ID);

    verify(userRepository).findByUsernameAndDeletedAtIsNull(TEST_USERNAME_LAURA);
  }

  @Test
  void validateUsernameIsAvailableForUpdateThrowsConflictWhenUsernameBelongsToAnotherUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_VALID, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID + 1);

    when(userRepository.findByUsernameAndDeletedAtIsNull(TEST_USERNAME_VALID))
        .thenReturn(Optional.of(user));

    UserException exception =
        assertThrows(
            UserException.class,
            () ->
                userValidationHelper.validateUsernameIsAvailableForUpdate(
                    TEST_USERNAME_VALID, TEST_USER_ID));

    assertEquals(ErrorDefinition.USER_USERNAME_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, UserMetricEnum.USERNAME_CONFLICT.metricName()));
  }

  @Test
  void validateCurrentPasswordThrowsWhenPasswordDoesNotMatch() {
    when(passwordEncoder.matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD)).thenReturn(false);

    UserException exception =
        assertThrows(
            UserException.class,
            () ->
                userValidationHelper.validateCurrentPassword(
                    TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD));

    assertEquals(ErrorDefinition.USER_CURRENT_PASSWORD_INVALID, exception.getErrorDefinition());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    assertEquals(
        1.0, counterValue(meterRegistry, UserMetricEnum.CURRENT_PASSWORD_INVALID.metricName()));
  }

  @Test
  void validateNewPasswordIsDifferentThrowsWhenNewPasswordMatchesCurrentOne() {
    when(passwordEncoder.matches(TEST_PASSWORD_NEW_VALID, TEST_ENCODED_PASSWORD)).thenReturn(true);

    UserException exception =
        assertThrows(
            UserException.class,
            () ->
                userValidationHelper.validateNewPasswordIsDifferent(
                    TEST_PASSWORD_NEW_VALID, TEST_ENCODED_PASSWORD));

    assertEquals(
        ErrorDefinition.USER_NEW_PASSWORD_MUST_BE_DIFFERENT, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, UserMetricEnum.NEW_PASSWORD_REUSED.metricName()));
  }

  @Test
  void validatePasswordUpdateChecksCurrentAndNewPasswordRules() {
    UserPasswordUpdateRequestDto requestDto =
        new UserPasswordUpdateRequestDto(
            TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);

    when(passwordEncoder.matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD)).thenReturn(true);
    when(passwordEncoder.matches(TEST_PASSWORD_NEW_VALID, TEST_ENCODED_PASSWORD)).thenReturn(false);

    userValidationHelper.validatePasswordUpdate(requestDto, user);

    verify(passwordEncoder).matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD);
    verify(passwordEncoder).matches(TEST_PASSWORD_NEW_VALID, TEST_ENCODED_PASSWORD);
  }

  @Test
  void validatePasswordUpdateShortCircuitsWhenCurrentPasswordIsInvalid() {
    UserPasswordUpdateRequestDto requestDto =
        new UserPasswordUpdateRequestDto(
            TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);

    when(passwordEncoder.matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD)).thenReturn(false);

    assertThrows(
        UserException.class, () -> userValidationHelper.validatePasswordUpdate(requestDto, user));

    verify(passwordEncoder).matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD);
    verify(passwordEncoder, never()).matches(TEST_PASSWORD_NEW_VALID, TEST_ENCODED_PASSWORD);
  }
}
