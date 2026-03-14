package com.tychewealth.service.impl;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.UserService;
import com.tychewealth.service.helper.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.AuthTokenHelper;
import com.tychewealth.service.helper.UserValidationHelper;
import java.time.LocalDateTime;
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
  private final AuthRefreshTokenHelper authRefreshTokenHelper;
  private final UserValidationHelper userValidationHelper;

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

    userValidationHelper.validateUsernameIsAvailableForUpdate(updateRequest.getUsername(), id);
    userMapper.update(updateRequest, user);

    return userMapper.toDto(userRepository.save(user));
  }

  @Override
  @Transactional
  public Long delete(String authorizationHeader) {
    Long id = authTokenHelper.extractUserId(authorizationHeader);
    UserEntity user = findActiveUser(id);

    authRefreshTokenHelper.revokeActiveTokensByUserId(id);

    user.setDeletedAt(LocalDateTime.now());
    userRepository.save(user);

    return user.getId();
  }

  private UserEntity findActiveUser(Long id) {
    return userRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () -> new UserException(ErrorDefinition.USER_NOT_FOUND, null, HttpStatus.NOT_FOUND));
  }
}
