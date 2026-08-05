package com.tychewealth.service.helper.auth;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.email.VerificationEmailWorkflow;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.utils.Utils;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Coordinates the resend flow for registration verification emails.
 *
 * <p>Loads the active user with a write lock, prevents duplicate delivery while a previous token
 * remains available, generates a replacement verification token, and delegates delivery and expiry
 * handling to {@link VerificationEmailWorkflow}.
 */
@Component
@AllArgsConstructor
public class AuthResendVerificationEmailHelper {

  private final UserRepository userRepository;
  private final AuthValidationHelper authValidationHelper;
  private final AccessTokenCodec accessTokenCodec;
  private final VerificationEmailWorkflow verificationEmailWorkflow;

  public void resendVerificationEmail(
      ResendVerificationEmailRequestDto resendVerificationEmailRequestDto) {
    String normalizedEmail = Utils.normalizeIdentity(resendVerificationEmailRequestDto.getEmail());
    UserEntity user =
        userRepository.findByEmailAndDeletedAtIsNullForUpdate(normalizedEmail).orElse(null);

    if (user == null || !authValidationHelper.canResendVerificationEmail(user)) {
      return;
    }

    Instant previousVerificationTokenExpiresAt = user.getVerificationTokenExpiresAt();
    AuthTokenDto verificationToken =
        accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL);
    Instant failedAttemptExpiry = accessTokenCodec.extractExpiration(verificationToken.token());
    user.setVerificationTokenExpiresAt(failedAttemptExpiry);
    verificationEmailWorkflow.scheduleVerificationEmail(
        user.getId(),
        user.getEmail(),
        verificationToken,
        failedAttemptExpiry,
        previousVerificationTokenExpiresAt,
        () -> {});
  }
}
