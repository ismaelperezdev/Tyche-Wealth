package com.tychewealth.service.helper.user;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.UserException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.auth.AuthRefreshTokenHelper;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class UserHelper {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final AuthRefreshTokenHelper authRefreshTokenHelper;
  private final PasswordEncoder passwordEncoder;
  private final UserMetrics userMetrics;

  public UserEntity findActiveUser(Long id) {
    return userRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () -> {
              log.warn(
                  LogConstants.REQUEST_CONFLICT,
                  LogConstants.USER,
                  LogConstants.RETRIEVE_ACTION,
                  "user not found");
              userMetrics.recordNotFound();
              return new UserException(ErrorDefinition.USER_NOT_FOUND, null, HttpStatus.NOT_FOUND);
            });
  }

  public UserEntity update(UserEntity user, UserUpdateRequestDto updateRequest) {
    userMapper.update(updateRequest, user);
    return userRepository.save(user);
  }

  @Transactional
  public Long updatePassword(UserEntity user, UserPasswordUpdateRequestDto updatePasswordRequest) {
    user.setPassword(passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
    userRepository.save(user);
    authRefreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    return user.getId();
  }

  @Transactional
  public Long softDelete(UserEntity user) {
    authRefreshTokenHelper.revokeActiveTokensByUserId(user.getId());
    user.setDeletedAt(LocalDateTime.now());
    userRepository.save(user);
    return user.getId();
  }
}
