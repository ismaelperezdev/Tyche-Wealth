package com.tychewealth.service.helper.user;

import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.AuthRefreshTokenHelper;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserHelper {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final UserValidationHelper userValidationHelper;
  private final AuthRefreshTokenHelper authRefreshTokenHelper;
  private final PasswordEncoder passwordEncoder;

  public UserEntity update(UserEntity user, UserUpdateRequestDto updateRequest) {
    userValidationHelper.validateUsernameIsAvailableForUpdate(
        updateRequest.getUsername(), user.getId());
    userMapper.update(updateRequest, user);
    return userRepository.save(user);
  }

  public Long updatePassword(UserEntity user, UserPasswordUpdateRequestDto updatePasswordRequest) {
    userValidationHelper.validateCurrentPassword(
        updatePasswordRequest.getCurrentPassword(), user.getPassword());
    userValidationHelper.validateNewPasswordIsDifferent(
        updatePasswordRequest.getNewPassword(), user.getPassword());
    user.setPassword(passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
    userRepository.save(user);
    authRefreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    return user.getId();
  }

  public Long softDelete(UserEntity user) {
    authRefreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    user.setDeletedAt(LocalDateTime.now());
    userRepository.save(user);
    return user.getId();
  }
}
