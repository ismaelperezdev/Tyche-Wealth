package com.tychewealth.service.helper.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
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
    when(userRepository.findByEmailAndDeletedAtIsNull("missing@tychewealth.com"))
        .thenReturn(Optional.empty());

    authForgotPasswordHelper.forgotPassword(
        new ResendVerificationEmailRequestDto("missing@tychewealth.com"));

    verify(emailSender, never()).send(any());
  }

  @Test
  void forgotPasswordDoesNotResendEmailWhilePreviousTokenIsStillActive() {
    UserEntity user = new UserEntity();
    user.setId(42L);
    user.setEmail("laura.gomez@tychewealth.com");

    AuthTokenDto token = new AuthTokenDto("Bearer", "forgot-token", 900L, "jti-1");
    EmailMessageDto emailMessage =
        new EmailMessageDto(user.getEmail(), "Reset your password", "<p>body</p>", "body");

    when(userRepository.findByEmailAndDeletedAtIsNull(user.getEmail()))
        .thenReturn(Optional.of(user));
    when(accessTokenCodec.generateToken(user, AccessTokenType.FORGOT_PASSWORD)).thenReturn(token);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true)
        .thenReturn(false);
    when(authEmailFactory.buildForgotPasswordEmailMessage(
            user.getEmail(), token.accessToken(), token.expiresIn()))
        .thenReturn(emailMessage);

    var request = new ResendVerificationEmailRequestDto(user.getEmail());

    authForgotPasswordHelper.forgotPassword(request);
    authForgotPasswordHelper.forgotPassword(request);

    verify(emailSender, times(1)).send(emailMessage);
  }
}
