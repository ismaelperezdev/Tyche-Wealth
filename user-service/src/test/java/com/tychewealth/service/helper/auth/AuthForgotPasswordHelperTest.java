package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
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

  private static final String TEST_FORGOT_PASSWORD_TOKEN = "forgot-token";
  private static final long TEST_FORGOT_PASSWORD_TTL_SECONDS = 900L;
  private static final String TEST_EMAIL_SUBJECT = "Reset your password";
  private static final String TEST_EMAIL_HTML = "<p>body</p>";
  private static final String TEST_EMAIL_TEXT = "body";

  @Mock private UserRepository userRepository;
  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private AuthEmailFactory authEmailFactory;
  @Mock private EmailSender emailSender;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private AuthForgotPasswordHelper authForgotPasswordHelper;

  @Test
  void forgotPasswordReturnsSilentlyWhenUserDoesNotExist() {
    when(userRepository.findByEmailAndDeletedAtIsNull("missing@tychewealth.com"))
        .thenReturn(Optional.empty());

    authForgotPasswordHelper.forgotPassword(
        new ForgotPasswordRequestDto("missing@tychewealth.com"));

    verify(emailSender, never()).send(any());
  }

  @Test
  void forgotPasswordDoesNotResendEmailWhilePreviousTokenIsStillActive() {
    UserEntity user = new UserEntity();
    user.setId(42L);
    user.setEmail("laura.gomez@tychewealth.com");

    AuthTokenDto token =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_FORGOT_PASSWORD_TOKEN,
            TEST_FORGOT_PASSWORD_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(user.getEmail(), TEST_EMAIL_SUBJECT, TEST_EMAIL_HTML, TEST_EMAIL_TEXT);

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
