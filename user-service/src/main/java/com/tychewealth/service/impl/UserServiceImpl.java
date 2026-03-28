package com.tychewealth.service.impl;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.service.UserService;
import com.tychewealth.service.helper.user.UserHelper;
import com.tychewealth.service.helper.user.UserValidationHelper;
import com.tychewealth.service.token.TokenStateStore;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final UserHelper userHelper;
  private final UserValidationHelper userValidationHelper;
  private final TokenStateStore tokenStateStore;

  @Override
  public UserResponseDto retrieve(Long userId) {
    return userMapper.toDto(userHelper.findActiveUser(userId));
  }

  @Override
  @Transactional
  public UserResponseDto update(Long userId, UserUpdateRequestDto updateRequest) {
    UserEntity user = userHelper.findActiveUser(userId);
    userValidationHelper.validateUsernameIsAvailableForUpdate(
        updateRequest.getUsername(), user.getId());

    return userMapper.toDto(userHelper.update(user, updateRequest));
  }

  @Override
  @Transactional
  public Long updatePassword(
      Long userId, String authorizationHeader, UserPasswordUpdateRequestDto updatePasswordRequest) {
    UserEntity user = userHelper.findActiveUser(userId);
    userValidationHelper.validatePasswordUpdate(updatePasswordRequest, user);
    revokeAccessTokenAfterCommit(authorizationHeader);

    return userHelper.updatePassword(user, updatePasswordRequest);
  }

  @Override
  @Transactional
  public Long delete(Long userId, String authorizationHeader) {
    UserEntity user = userHelper.findActiveUser(userId);
    revokeAccessTokenAfterCommit(authorizationHeader);

    return userHelper.softDelete(user);
  }

  private void revokeAccessTokenAfterCommit(String authorizationHeader) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      tokenStateStore.revokeAccessTokenIfPresent(authorizationHeader);
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            tokenStateStore.revokeAccessTokenIfPresent(authorizationHeader);
          }
        });
  }
}
