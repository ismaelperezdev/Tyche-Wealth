package com.tychewealth.service.email;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.repository.UserRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;

@Slf4j
@Component
public class VerificationEmailWorkflow {

  private final AuthEmailFactory authEmailFactory;
  private final EmailSender emailSender;
  private final UserRepository userRepository;
  private final VerificationEmailWorkflow self;

  public VerificationEmailWorkflow(
      AuthEmailFactory authEmailFactory,
      EmailSender emailSender,
      UserRepository userRepository,
      @Lazy VerificationEmailWorkflow self) {
    this.authEmailFactory = authEmailFactory;
    this.emailSender = emailSender;
    this.userRepository = userRepository;
    this.self = self;
  }

  public void scheduleVerificationEmail(
      Long userId,
      String email,
      AuthTokenDto verificationToken,
      Instant failedAttemptExpiry,
      Instant previousVerificationTokenExpiresAt,
      Runnable onSuccess) {

    var verificationEmailMessage =
        authEmailFactory.buildVerifyEmailMessage(
            email, verificationToken.token(), verificationToken.expiresIn());

    registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            handleVerificationEmailAfterCommit(
                userId,
                failedAttemptExpiry,
                previousVerificationTokenExpiresAt,
                verificationEmailMessage,
                onSuccess);
          }
        });
  }

  private void handleVerificationEmailAfterCommit(
      Long userId,
      Instant failedAttemptExpiry,
      Instant previousVerificationTokenExpiresAt,
      EmailMessageDto verificationEmailMessageDto,
      Runnable onSuccess) {
    try {
      emailSender.send(verificationEmailMessageDto);
    } catch (RuntimeException ex) {
      self.restoreVerificationTokenExpiryWithErrorHandling(
          userId, failedAttemptExpiry, previousVerificationTokenExpiresAt);
      throw ex;
    }

    onSuccess.run();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void restoreVerificationTokenExpiryWithErrorHandling(
      Long userId, Instant failedAttemptExpiry, Instant previousVerificationTokenExpiresAt) {
    try {
      userRepository
          .findById(userId)
          .ifPresent(
              user -> {
                if (failedAttemptExpiry == null) {
                  return;
                }
                if (!failedAttemptExpiry.equals(user.getVerificationTokenExpiresAt())) {
                  return;
                }
                user.setVerificationTokenExpiresAt(previousVerificationTokenExpiresAt);
                userRepository.save(user);
              });
    } catch (RuntimeException ex) {
      log.error(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.EMAIL,
          LogConstants.SEND_ACTION,
          "failed to restore verification token expiry",
          userId,
          ex);
    }
  }
}
