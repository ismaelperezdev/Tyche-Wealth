package com.tychewealth.service.helper.email;

import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.EmailService;
import com.tychewealth.service.token.AuthTokenPayload;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@AllArgsConstructor
public class VerificationEmailHelper {

  private final RegisterEmailHelper registerEmailHelper;
  private final EmailService emailService;
  private final UserRepository userRepository;

  public void scheduleVerificationEmail(
      Long userId,
      String email,
      AuthTokenPayload verificationToken,
      Instant previousVerificationTokenExpiresAt,
      Runnable onSuccess) {

    var verificationEmailMessage =
        registerEmailHelper.buildVerifyEmailMessage(
            email, verificationToken.accessToken(), verificationToken.expiresIn());

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              emailService.send(verificationEmailMessage);
              onSuccess.run();
            } catch (RuntimeException ex) {
              restoreVerificationTokenExpiry(userId, previousVerificationTokenExpiresAt);
              throw ex;
            }
          }
        });
  }

  private void restoreVerificationTokenExpiry(
      Long userId, Instant previousVerificationTokenExpiresAt) {
    userRepository
        .findById(userId)
        .ifPresent(
            user -> {
              user.setVerificationTokenExpiresAt(previousVerificationTokenExpiresAt);
              userRepository.save(user);
            });
  }
}
