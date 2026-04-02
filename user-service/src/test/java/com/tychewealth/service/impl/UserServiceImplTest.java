package com.tychewealth.service.impl;

import static com.tychewealth.constants.TestConstants.TEST_BEARER_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_ENCODED_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_UPDATE_USERNAME_REQUEST;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.service.helper.user.UserHelper;
import com.tychewealth.service.helper.user.UserValidationHelper;
import com.tychewealth.service.token.TokenStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserMapper userMapper;
  @Mock private UserHelper userHelper;
  @Mock private UserValidationHelper userValidationHelper;
  @Mock private TokenStateStore tokenStateStore;

  @InjectMocks private UserServiceImpl userService;

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void retrieveFindsActiveUserAndMapsToDto() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    UserResponseDto responseDto =
        new UserResponseDto(TEST_USER_ID, TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);

    when(userHelper.findActiveUser(TEST_USER_ID)).thenReturn(user);
    when(userMapper.toDto(user)).thenReturn(responseDto);

    UserResponseDto result = userService.retrieve(TEST_USER_ID);

    assertSame(responseDto, result);
    verify(userHelper).findActiveUser(TEST_USER_ID);
    verify(userMapper).toDto(user);
  }

  @Test
  void updateFindsValidatesUpdatesAndMapsUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);
    UserUpdateRequestDto requestDto = new UserUpdateRequestDto(TEST_UPDATE_USERNAME_REQUEST);
    UserEntity updatedUser =
        buildUser(TEST_EMAIL_LAURA, TEST_UPDATE_USERNAME_REQUEST, TEST_ENCODED_PASSWORD);
    UserResponseDto responseDto =
        new UserResponseDto(TEST_USER_ID, TEST_EMAIL_LAURA, TEST_UPDATE_USERNAME_REQUEST, null);

    when(userHelper.findActiveUser(TEST_USER_ID)).thenReturn(user);
    when(userHelper.update(user, requestDto)).thenReturn(updatedUser);
    when(userMapper.toDto(updatedUser)).thenReturn(responseDto);

    UserResponseDto result = userService.update(TEST_USER_ID, requestDto);

    assertSame(responseDto, result);
    verify(userValidationHelper)
        .validateUsernameIsAvailableForUpdate(requestDto.getUsername(), TEST_USER_ID);
    verify(userHelper).update(user, requestDto);
    verify(userMapper).toDto(updatedUser);
  }

  @Test
  void updatePasswordValidatesSchedulesRevocationAndDelegatesToHelper() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);
    UserPasswordUpdateRequestDto requestDto =
        new UserPasswordUpdateRequestDto(
            TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID);

    when(userHelper.findActiveUser(TEST_USER_ID)).thenReturn(user);
    when(userHelper.updatePassword(user, requestDto)).thenReturn(TEST_USER_ID);

    TransactionSynchronizationManager.initSynchronization();

    Long result = userService.updatePassword(TEST_USER_ID, TEST_BEARER_ACCESS_TOKEN, requestDto);

    assertEquals(TEST_USER_ID, result);
    verify(userValidationHelper).validatePasswordUpdate(requestDto, user);
    verify(userHelper).updatePassword(user, requestDto);
    verify(tokenStateStore, never()).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);

    runAfterCommitCallbacks();

    verify(tokenStateStore).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);
  }

  @Test
  void updatePasswordRevokesImmediatelyWhenNoTransactionSynchronizationIsActive() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);
    UserPasswordUpdateRequestDto requestDto =
        new UserPasswordUpdateRequestDto(
            TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID);

    when(userHelper.findActiveUser(TEST_USER_ID)).thenReturn(user);
    when(userHelper.updatePassword(user, requestDto)).thenReturn(TEST_USER_ID);

    userService.updatePassword(TEST_USER_ID, TEST_BEARER_ACCESS_TOKEN, requestDto);

    verify(tokenStateStore).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);
  }

  @Test
  void deleteFindsUserSchedulesRevocationAndDelegatesToHelper() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);

    when(userHelper.findActiveUser(TEST_USER_ID)).thenReturn(user);
    when(userHelper.softDelete(user)).thenReturn(TEST_USER_ID);

    TransactionSynchronizationManager.initSynchronization();

    Long result = userService.delete(TEST_USER_ID, TEST_BEARER_ACCESS_TOKEN);

    assertEquals(TEST_USER_ID, result);
    verify(userHelper).softDelete(user);
    verify(tokenStateStore, never()).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);

    runAfterCommitCallbacks();

    verify(tokenStateStore).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);
  }

  @Test
  void deleteSwallowsErrorsWhenAfterCommitRevocationFails() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_ENCODED_PASSWORD);
    user.setId(TEST_USER_ID);

    when(userHelper.findActiveUser(TEST_USER_ID)).thenReturn(user);
    when(userHelper.softDelete(user)).thenReturn(TEST_USER_ID);
    doThrow(new IllegalStateException("redis unavailable"))
        .when(tokenStateStore)
        .revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);

    TransactionSynchronizationManager.initSynchronization();

    userService.delete(TEST_USER_ID, TEST_BEARER_ACCESS_TOKEN);

    runAfterCommitCallbacks();

    verify(tokenStateStore).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);
  }

  private void runAfterCommitCallbacks() {
    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations()) {
      synchronization.afterCommit();
    }
  }
}
