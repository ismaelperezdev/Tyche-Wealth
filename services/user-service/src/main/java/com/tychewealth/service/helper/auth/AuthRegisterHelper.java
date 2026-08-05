package com.tychewealth.service.helper.auth;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.RegisteredUserResultDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.token.AccessTokenCodec;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Builds and persists new user accounts during registration.
 *
 * <p>Maps the registration request to a {@link UserEntity}, encodes the password, creates the
 * email-verification token and expiry, and returns both the public user representation and token
 * required by the registration email workflow.
 */
@Component
@AllArgsConstructor
public class AuthRegisterHelper {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenCodec accessTokenCodec;

  public RegisteredUserResultDto createUser(RegisterRequestDto register) {
    UserEntity toCreate = userMapper.create(register);
    toCreate.setPassword(passwordEncoder.encode(register.getPassword()));
    UserEntity created = userRepository.save(toCreate);

    AuthTokenDto verificationToken =
        accessTokenCodec.generateToken(created, AccessTokenType.VERIFY_EMAIL);
    Instant verificationTokenExpiresAt =
        accessTokenCodec.extractExpiration(verificationToken.token());
    created.setVerificationTokenExpiresAt(verificationTokenExpiresAt);

    UserResponseDto response = userMapper.toDto(created);
    return new RegisteredUserResultDto(response, verificationToken);
  }
}
