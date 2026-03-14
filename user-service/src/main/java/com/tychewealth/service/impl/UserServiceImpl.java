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
import com.tychewealth.service.helper.AuthTokenHelper;
import com.tychewealth.service.helper.user.UserHelper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final AuthTokenHelper authTokenHelper;
  private final UserHelper userHelper;

  @Override
  public UserResponseDto retrieve(String authorizationHeader) {
    Long id = authTokenHelper.extractUserId(authorizationHeader);

    return userMapper.toDto(findActiveUser(id));
  }

  @Override
  @Transactional
  public UserResponseDto update(String authorizationHeader, UserUpdateRequestDto updateRequest) {
    Long id = authTokenHelper.extractUserId(authorizationHeader);
    UserEntity user = findActiveUser(id);

    return userMapper.toDto(userHelper.update(user, updateRequest));
  }

  @Override
  @Transactional
  public Long updatePassword(
      String authorizationHeader, UserPasswordUpdateRequestDto updatePasswordRequest) {
    Long id = authTokenHelper.extractUserId(authorizationHeader);
    UserEntity user = findActiveUser(id);

    return userHelper.updatePassword(user, updatePasswordRequest);
  }

  @Override
  @Transactional
  public Long delete(String authorizationHeader) {
    Long id = authTokenHelper.extractUserId(authorizationHeader);
    UserEntity user = findActiveUser(id);

    return userHelper.softDelete(user);
  }

  private UserEntity findActiveUser(Long id) {
    return userRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () -> new UserException(ErrorDefinition.USER_NOT_FOUND, null, HttpStatus.NOT_FOUND));
  }
}
