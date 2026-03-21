package com.tychewealth.service.helper.email;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

import com.tychewealth.service.EmailService;
import com.tychewealth.service.email.EmailMessage;
import com.tychewealth.service.helper.token.VerificationTokenRecoveryHelper;
import com.tychewealth.service.token.AuthTokenPayload;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

@Component
@AllArgsConstructor
public class VerificationEmailHelper {

  private final RegisterEmailHelper registerEmailHelper;
  private final EmailService emailService;
  private final VerificationTokenRecoveryHelper verificationTokenRecoveryHelper;

  public void scheduleVerificationEmail(
      Long userId,
      String email,
      AuthTokenPayload verificationToken,
      Instant previousVerificationTokenExpiresAt,
      Runnable onSuccess) {

    var verificationEmailMessage =
        registerEmailHelper.buildVerifyEmailMessage(
            email, verificationToken.accessToken(), verificationToken.expiresIn());

    registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            handleVerificationEmailAfterCommit(
                userId, previousVerificationTokenExpiresAt, verificationEmailMessage, onSuccess);
          }
        });
  }

  private void handleVerificationEmailAfterCommit(
      Long userId,
      Instant previousVerificationTokenExpiresAt,
      EmailMessage verificationEmailMessage,
      Runnable onSuccess) {
    try {
      emailService.send(verificationEmailMessage);
    } catch (RuntimeException ex) {
      verificationTokenRecoveryHelper.restoreVerificationTokenExpiryWithErrorHandling(
          userId, previousVerificationTokenExpiresAt);
      throw ex;
    }

    onSuccess.run();
  }
}
