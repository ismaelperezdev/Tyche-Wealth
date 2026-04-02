package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.EMAIL_CONSTRAINT;
import static com.tychewealth.constants.AuthConstants.USERNAME_CONSTRAINT;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_VALID;
import static com.tychewealth.constants.TestConstants.TEST_ENCODED_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_TOO_SHORT;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.MetricsTestHelper.counterValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthValidationHelperTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private SimpleMeterRegistry meterRegistry;
  private AuthValidationHelper authValidationHelper;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    authValidationHelper =
        new AuthValidationHelper(userRepository, passwordEncoder, new AuthMetrics(meterRegistry));
  }

  @Test
  void validateRegisterRequestPassesWhenEmailUsernameAndPasswordAreValid() {
    RegisterRequestDto requestDto =
        new RegisterRequestDto(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);

    when(userRepository.findByEmailIncludingDeleted(TEST_EMAIL_LAURA)).thenReturn(Optional.empty());
    when(userRepository.findByUsernameIncludingDeleted(TEST_USERNAME_LAURA))
        .thenReturn(Optional.empty());

    authValidationHelper.validateRegisterRequest(requestDto);

    verify(userRepository).findByEmailIncludingDeleted(TEST_EMAIL_LAURA);
    verify(userRepository).findByUsernameIncludingDeleted(TEST_USERNAME_LAURA);
  }

  @Test
  void validateEmailIsAvailableThrowsConflictWhenEmailAlreadyExists() {
    UserEntity existingUser =
        buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    when(userRepository.findByEmailIncludingDeleted(TEST_EMAIL_LAURA))
        .thenReturn(Optional.of(existingUser));

    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authValidationHelper.validateEmailIsAvailable(TEST_EMAIL_LAURA));

    assertEquals(ErrorDefinition.AUTH_REGISTRATION_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_FAILURE.metricName()));
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_CONFLICT.metricName()));
  }

  @Test
  void validateUsernameIsAvailableThrowsConflictWhenUsernameAlreadyExists() {
    UserEntity existingUser =
        buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    when(userRepository.findByUsernameIncludingDeleted(TEST_USERNAME_LAURA))
        .thenReturn(Optional.of(existingUser));

    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authValidationHelper.validateUsernameIsAvailable(TEST_USERNAME_LAURA));

    assertEquals(ErrorDefinition.AUTH_REGISTRATION_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_FAILURE.metricName()));
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_CONFLICT.metricName()));
  }

  @Test
  void validateRegisterPasswordFormatThrowsWhenPasswordIsInvalid() {
    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authValidationHelper.validateRegisterPasswordFormat(TEST_PASSWORD_TOO_SHORT));

    assertEquals(
        ErrorDefinition.AUTH_REGISTER_PASSWORD_FORMAT_INVALID, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_FAILURE.metricName()));
  }

  @Test
  void validateLoginRequestReturnsUserWhenCredentialsAreValid() {
    LoginRequestDto requestDto = new LoginRequestDto(TEST_EMAIL_VALID, TEST_PASSWORD_VALID);
    UserEntity user = buildUser(TEST_EMAIL_VALID, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setVerified(true);

    when(userRepository.findByEmailAndDeletedAtIsNull(TEST_EMAIL_VALID))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD)).thenReturn(true);

    UserEntity result = authValidationHelper.validateLoginRequest(requestDto);

    assertSame(user, result);
    verify(passwordEncoder).matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD);
  }

  @Test
  void validateLoginRequestThrowsWhenEmailDoesNotExist() {
    LoginRequestDto requestDto = new LoginRequestDto(TEST_EMAIL_VALID, TEST_PASSWORD_VALID);
    when(userRepository.findByEmailAndDeletedAtIsNull(TEST_EMAIL_VALID))
        .thenReturn(Optional.empty());

    AuthException exception =
        assertThrows(
            AuthException.class, () -> authValidationHelper.validateLoginRequest(requestDto));

    assertEquals(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, exception.getErrorDefinition());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_FAILURE.metricName()));
    assertEquals(
        1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_INVALID_CREDENTIALS.metricName()));
  }

  @Test
  void validateLoginRequestThrowsWhenUserIsNotVerified() {
    LoginRequestDto requestDto = new LoginRequestDto(TEST_EMAIL_VALID, TEST_PASSWORD_VALID);
    UserEntity user = buildUser(TEST_EMAIL_VALID, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setVerified(false);

    when(userRepository.findByEmailAndDeletedAtIsNull(TEST_EMAIL_VALID))
        .thenReturn(Optional.of(user));

    AuthException exception =
        assertThrows(
            AuthException.class, () -> authValidationHelper.validateLoginRequest(requestDto));

    assertEquals(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, exception.getErrorDefinition());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    verifyNoInteractions(passwordEncoder);
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_FAILURE.metricName()));
    assertEquals(
        1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_INVALID_CREDENTIALS.metricName()));
  }

  @Test
  void validateLoginPasswordThrowsWhenPasswordDoesNotMatch() {
    when(passwordEncoder.matches(TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD)).thenReturn(false);

    AuthException exception =
        assertThrows(
            AuthException.class,
            () ->
                authValidationHelper.validateLoginPassword(
                    TEST_PASSWORD_VALID, TEST_ENCODED_PASSWORD));

    assertEquals(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS, exception.getErrorDefinition());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_FAILURE.metricName()));
    assertEquals(
        1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_INVALID_CREDENTIALS.metricName()));
  }

  @Test
  void canResendVerificationEmailReturnsTrueWhenTokenIsMissing() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setVerified(false);
    user.setVerificationTokenExpiresAt(null);

    assertTrue(authValidationHelper.canResendVerificationEmail(user));
  }

  @Test
  void canResendVerificationEmailReturnsTrueWhenTokenIsExpired() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setVerified(false);
    user.setVerificationTokenExpiresAt(Instant.now().minusSeconds(60));

    assertTrue(authValidationHelper.canResendVerificationEmail(user));
  }

  @Test
  void validateRegisterPersistenceConflictReturnsAuthExceptionForEmailConstraint() {
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException("duplicate key for " + EMAIL_CONSTRAINT);

    AuthException result =
        assertThrows(
            AuthException.class,
            () -> {
              throw authValidationHelper.validateRegisterPersistenceConflict(exception);
            });

    assertEquals(ErrorDefinition.AUTH_REGISTRATION_CONFLICT, result.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, result.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_FAILURE.metricName()));
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_CONFLICT.metricName()));
  }

  @Test
  void validateRegisterPersistenceConflictReturnsAuthExceptionForUsernameConstraintInCause() {
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException(
            "persistence failure", new IllegalStateException("constraint " + USERNAME_CONSTRAINT));

    AuthException result =
        assertThrows(
            AuthException.class,
            () -> {
              throw authValidationHelper.validateRegisterPersistenceConflict(exception);
            });

    assertEquals(ErrorDefinition.AUTH_REGISTRATION_CONFLICT, result.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, result.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_FAILURE.metricName()));
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_CONFLICT.metricName()));
  }

  @Test
  void validateRegisterPersistenceConflictRethrowsOriginalExceptionWhenConstraintIsUnrelated() {
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException("some unrelated persistence failure");

    DataIntegrityViolationException thrown =
        assertThrows(
            DataIntegrityViolationException.class,
            () -> authValidationHelper.validateRegisterPersistenceConflict(exception));

    assertSame(exception, thrown);
    assertEquals(0.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_FAILURE.metricName()));
    assertEquals(0.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_CONFLICT.metricName()));
  }
}
