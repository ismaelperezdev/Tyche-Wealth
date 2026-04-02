package com.tychewealth.service.helper.user;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_ENCODED_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.MetricsTestHelper.counterValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.UserMetricEnum;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.auth.AuthRefreshTokenHelper;
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
class UserHelperTest {

  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;
  @Mock private AuthRefreshTokenHelper authRefreshTokenHelper;
  @Mock private PasswordEncoder passwordEncoder;

  private SimpleMeterRegistry meterRegistry;
  private UserHelper userHelper;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    userHelper =
        new UserHelper(
            userRepository,
            userMapper,
            authRefreshTokenHelper,
            passwordEncoder,
            new UserMetrics(meterRegistry));
  }

  @Test
  void findActiveUserReturnsUserWhenItExists() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);

    when(userRepository.findByIdAndDeletedAtIsNull(TEST_USER_ID)).thenReturn(Optional.of(user));

    UserEntity result = userHelper.findActiveUser(TEST_USER_ID);

    assertSame(user, result);
  }

  @Test
  void findActiveUserThrowsNotFoundWhenUserDoesNotExist() {
    when(userRepository.findByIdAndDeletedAtIsNull(TEST_USER_ID)).thenReturn(Optional.empty());

    UserException exception =
        assertThrows(UserException.class, () -> userHelper.findActiveUser(TEST_USER_ID));

    assertEquals(ErrorDefinition.USER_NOT_FOUND, exception.getErrorDefinition());
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, UserMetricEnum.NOT_FOUND.metricName()));
  }

  @Test
  void updateMapsRequestAndSavesUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    UserUpdateRequestDto requestDto = new UserUpdateRequestDto(TEST_USERNAME_VALID);

    when(userRepository.save(user)).thenReturn(user);

    UserEntity result = userHelper.update(user, requestDto);

    assertSame(user, result);
    verify(userMapper).update(requestDto, user);
    verify(userRepository).save(user);
  }

  @Test
  void updatePasswordEncodesPasswordSavesUserAndRevokesRefreshTokens() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);
    UserPasswordUpdateRequestDto requestDto =
        new UserPasswordUpdateRequestDto(
            TEST_ENCODED_PASSWORD, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID);

    when(passwordEncoder.encode(TEST_PASSWORD_NEW_VALID)).thenReturn("new-encoded-password");

    Long result = userHelper.updatePassword(user, requestDto);

    assertEquals(TEST_USER_ID, result);
    assertEquals("new-encoded-password", user.getPassword());
    verify(userRepository).save(user);
    verify(authRefreshTokenHelper).revokeActiveTokensByUserId(TEST_USER_ID);
  }

  @Test
  void softDeleteRevokesRefreshTokensSetsDeletedAtAndSavesUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);

    Long result = userHelper.softDelete(user);

    assertEquals(TEST_USER_ID, result);
    assertNotNull(user.getDeletedAt());
    verify(authRefreshTokenHelper).revokeActiveTokensByUserId(TEST_USER_ID);
    verify(userRepository).save(user);
  }
}
