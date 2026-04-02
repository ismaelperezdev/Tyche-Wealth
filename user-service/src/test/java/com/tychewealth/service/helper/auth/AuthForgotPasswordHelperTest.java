package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_HTML_BODY;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_SUBJECT_RESET_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_TEXT_BODY;
import static com.tychewealth.constants.TestConstants.TEST_FORGOT_PASSWORD_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_MISSING_EMAIL;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.email.AuthEmailFactory;
import com.tychewealth.service.token.AccessTokenCodec;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AuthForgotPasswordHelperTest {

  @Mock private UserRepository userRepository;
  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private AuthEmailFactory authEmailFactory;
  @Mock private EmailSender emailSender;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private AuthForgotPasswordHelper authForgotPasswordHelper;

  @Test
  void forgotPasswordReturnsSilentlyWhenUserDoesNotExist() {
    when(userRepository.findByEmailAndDeletedAtIsNull(TEST_MISSING_EMAIL))
        .thenReturn(Optional.empty());

    authForgotPasswordHelper.forgotPassword(new ForgotPasswordRequestDto(TEST_MISSING_EMAIL));

    verify(emailSender, never()).send(any());
  }

  @Test
  void forgotPasswordDoesNotResendEmailWhilePreviousTokenIsStillActive() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TEST_USER_ID);

    AuthTokenDto token =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_FORGOT_PASSWORD_TOKEN,
            TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(
            user.getEmail(),
            TEST_EMAIL_SUBJECT_RESET_PASSWORD,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY);

    when(userRepository.findByEmailAndDeletedAtIsNull(user.getEmail()))
        .thenReturn(Optional.of(user));
    when(accessTokenCodec.generateToken(user, AccessTokenType.FORGOT_PASSWORD)).thenReturn(token);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true)
        .thenReturn(false);
    when(authEmailFactory.buildForgotPasswordEmailMessage(
            user.getEmail(), token.token(), token.expiresIn()))
        .thenReturn(emailMessage);

    var request = new ForgotPasswordRequestDto(user.getEmail());

    authForgotPasswordHelper.forgotPassword(request);
    authForgotPasswordHelper.forgotPassword(request);

    verify(emailSender, times(1)).send(emailMessage);
  }
}
