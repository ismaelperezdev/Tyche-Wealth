package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.FORGOT_PASSWORD_TOKEN_PURPOSE;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.email.AuthEmailFactory;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.utils.Utils;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AuthForgotPasswordHelper {

  private final UserRepository userRepository;
  private final AccessTokenCodec accessTokenCodec;
  private final AuthEmailFactory authEmailFactory;
  private final EmailSender emailSender;
  private final StringRedisTemplate redisTemplate;

  public void forgotPassword(ResendVerificationEmailRequestDto requestDto) {
    String normalizedEmail = Utils.normalizeIdentity(requestDto.getEmail());
    UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail).orElse(null);

    if (user == null) {
      return;
    }

    AuthTokenDto forgotPasswordToken =
        accessTokenCodec.generateToken(user, AccessTokenType.FORGOT_PASSWORD);
    boolean stored =
        storeTokenIfAbsent(
            user.getId(),
            forgotPasswordToken.accessToken(),
            Duration.ofSeconds(forgotPasswordToken.expiresIn()));

    if (!stored) {
      return;
    }

    try {
      emailSender.send(
          authEmailFactory.buildForgotPasswordEmailMessage(
              user.getEmail(), forgotPasswordToken.accessToken(), forgotPasswordToken.expiresIn()));
    } catch (RuntimeException ex) {
      deleteToken(user.getId());
      throw ex;
    }
  }

  public boolean storeTokenIfAbsent(Long userId, String token, Duration ttl) {
    return Boolean.TRUE.equals(
        redisTemplate.opsForValue().setIfAbsent(forgotPasswordKey(userId), token, ttl));
  }

  public void deleteToken(Long userId) {
    redisTemplate.delete(forgotPasswordKey(userId));
  }

  private String forgotPasswordKey(Long userId) {
    return FORGOT_PASSWORD_TOKEN_PURPOSE + ": " + userId;
  }
}
