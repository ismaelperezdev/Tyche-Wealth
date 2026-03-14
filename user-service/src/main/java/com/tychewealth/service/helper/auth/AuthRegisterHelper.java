package com.tychewealth.service.helper.auth;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.monitoring.AuthMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthRegisterHelper {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthMetrics authMetrics;

  public UserResponseDto createUser(RegisterRequestDto register) {
    UserEntity toCreate = userMapper.create(register);
    toCreate.setPassword(passwordEncoder.encode(register.getPassword()));
    UserEntity created = userRepository.save(toCreate);
    UserResponseDto response = userMapper.toDto(created);
    authMetrics.recordRegisterSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.AUTH,
        LogConstants.REGISTER_ACTION,
        created.getId());
    return response;
  }
}
