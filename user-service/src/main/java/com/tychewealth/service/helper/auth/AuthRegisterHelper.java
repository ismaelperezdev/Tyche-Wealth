package com.tychewealth.service.helper.auth;

import com.tychewealth.dto.auth.RegisteredUserResultDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.token.AccessTokenHelper;
import com.tychewealth.service.token.AuthTokenPayload;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthRegisterHelper {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenHelper accessTokenHelper;

  public RegisteredUserResultDto createUser(RegisterRequestDto register) {
    UserEntity toCreate = userMapper.create(register);
    toCreate.setPassword(passwordEncoder.encode(register.getPassword()));
    UserEntity created = userRepository.save(toCreate);
    AuthTokenPayload verificationToken = accessTokenHelper.generateVerifyEmailToken(created);
    UserResponseDto response = userMapper.toDto(created);
    return new RegisteredUserResultDto(response, verificationToken);
  }
}
