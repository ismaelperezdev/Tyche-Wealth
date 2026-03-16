package com.tychewealth.service.impl;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.UserService;
import com.tychewealth.service.helper.user.UserHelper;
import com.tychewealth.service.monitoring.UserMetrics;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final UserHelper userHelper;
  private final UserMetrics userMetrics;

  @Override
  public UserResponseDto retrieve(Long userId) {
    return userMapper.toDto(findActiveUser(userId));
  }

  @Override
  @Transactional
  public UserResponseDto update(Long userId, UserUpdateRequestDto updateRequest) {
    UserEntity user = findActiveUser(userId);

    return userMapper.toDto(userHelper.update(user, updateRequest));
  }

  @Override
  @Transactional
  public Long updatePassword(Long userId, UserPasswordUpdateRequestDto updatePasswordRequest) {
    UserEntity user = findActiveUser(userId);

    return userHelper.updatePassword(user, updatePasswordRequest);
  }

  @Override
  @Transactional
  public Long delete(Long userId) {
    UserEntity user = findActiveUser(userId);

    return userHelper.softDelete(user);
  }

  private UserEntity findActiveUser(Long id) {
    return userRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () -> {
              userMetrics.recordNotFound();
              return new UserException(ErrorDefinition.USER_NOT_FOUND, null, HttpStatus.NOT_FOUND);
            });
  }
}
