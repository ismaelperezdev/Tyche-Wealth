package com.tychewealth.service.impl;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.service.UserService;
import com.tychewealth.service.helper.user.UserHelper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final UserHelper userHelper;

  @Override
  public UserResponseDto retrieve(Long userId) {
    return userMapper.toDto(userHelper.findActiveUser(userId));
  }

  @Override
  @Transactional
  public UserResponseDto update(Long userId, UserUpdateRequestDto updateRequest) {
    UserEntity user = userHelper.findActiveUser(userId);

    return userMapper.toDto(userHelper.update(user, updateRequest));
  }

  @Override
  @Transactional
  public Long updatePassword(Long userId, UserPasswordUpdateRequestDto updatePasswordRequest) {
    UserEntity user = userHelper.findActiveUser(userId);

    return userHelper.updatePassword(user, updatePasswordRequest);
  }

  @Override
  @Transactional
  public Long delete(Long userId) {
    UserEntity user = userHelper.findActiveUser(userId);

    return userHelper.softDelete(user);
  }
}
