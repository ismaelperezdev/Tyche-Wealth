package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_ENCODED_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthRegisterHelperTest {

  private static final Instant TEST_VERIFICATION_TOKEN_EXPIRES_AT =
      Instant.parse("2026-04-02T12:00:00Z");

  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AccessTokenCodec accessTokenCodec;

  @InjectMocks private AuthRegisterHelper authRegisterHelper;

  @Test
  void createUserEncodesPasswordPersistsUserAndReturnsVerificationToken() {
    RegisterRequestDto requestDto =
        new RegisterRequestDto(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);
    UserEntity toCreate = new UserEntity();
    UserEntity created = new UserEntity();
    created.setId(TEST_USER_ID);
    UserResponseDto responseDto =
        new UserResponseDto(TEST_USER_ID, TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_EMAIL_TOKEN,
            TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);

    when(userMapper.create(requestDto)).thenReturn(toCreate);
    when(passwordEncoder.encode(TEST_PASSWORD_VALID)).thenReturn(TEST_ENCODED_PASSWORD);
    when(userRepository.save(toCreate)).thenReturn(created);
    when(accessTokenCodec.generateToken(created, AccessTokenType.VERIFY_EMAIL))
        .thenReturn(verificationToken);
    when(accessTokenCodec.extractExpiration(TEST_VERIFY_EMAIL_TOKEN))
        .thenReturn(TEST_VERIFICATION_TOKEN_EXPIRES_AT);
    when(userMapper.toDto(created)).thenReturn(responseDto);

    RegisteredUserResultDto result = authRegisterHelper.createUser(requestDto);

    assertEquals(TEST_ENCODED_PASSWORD, toCreate.getPassword());
    assertEquals(TEST_VERIFICATION_TOKEN_EXPIRES_AT, created.getVerificationTokenExpiresAt());
    assertSame(responseDto, result.response());
    assertSame(verificationToken, result.verificationToken());

    verify(userMapper).create(requestDto);
    verify(passwordEncoder).encode(TEST_PASSWORD_VALID);
    verify(userRepository).save(toCreate);
    verify(accessTokenCodec).generateToken(created, AccessTokenType.VERIFY_EMAIL);
    verify(accessTokenCodec).extractExpiration(TEST_VERIFY_EMAIL_TOKEN);
    verify(userMapper).toDto(created);
  }
}
