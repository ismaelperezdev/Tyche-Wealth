package com.tychewealth.service.email;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.EmailSendResult;
import com.tychewealth.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class VerificationEmailWorkflowTest {

  private static final String TEST_VERIFY_EMAIL_TOKEN = "verify-email-token";
  private static final String TEST_EMAIL_SUBJECT = "Verify your email";
  private static final String TEST_EMAIL_HTML = "<p>body</p>";
  private static final String TEST_EMAIL_TEXT = "body";
  private static final Instant PREVIOUS_VERIFICATION_TOKEN_EXPIRY =
      Instant.parse("2026-04-01T10:15:30Z");
  private static final Instant FAILED_ATTEMPT_EXPIRY = Instant.parse("2026-04-01T11:15:30Z");
  private static final AuthTokenDto TEST_VERIFICATION_TOKEN =
      new AuthTokenDto(
          TOKEN_TYPE_BEARER,
          TEST_VERIFY_EMAIL_TOKEN,
          TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
          TEST_ACCESS_TOKEN_JTI);

  @Mock private AuthEmailFactory authEmailFactory;
  @Mock private EmailSender emailSender;
  @Mock private UserRepository userRepository;
  @Mock private VerificationEmailWorkflow self;

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void scheduleVerificationEmailSendsEmailAfterCommitAndRunsSuccessCallback() {
    VerificationEmailWorkflow verificationEmailWorkflow =
        new VerificationEmailWorkflow(authEmailFactory, emailSender, userRepository, self);
    EmailMessageDto emailMessage =
        new EmailMessageDto(TEST_EMAIL_LAURA, TEST_EMAIL_SUBJECT, TEST_EMAIL_HTML, TEST_EMAIL_TEXT);
    AtomicBoolean successCallbackInvoked = new AtomicBoolean(false);

    when(authEmailFactory.buildVerifyEmailMessage(
            TEST_EMAIL_LAURA, TEST_VERIFY_EMAIL_TOKEN, TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenReturn(EmailSendResult.DELIVERED);

    TransactionSynchronizationManager.initSynchronization();

    verificationEmailWorkflow.scheduleVerificationEmail(
        TEST_USER_ID,
        TEST_EMAIL_LAURA,
        TEST_VERIFICATION_TOKEN,
        FAILED_ATTEMPT_EXPIRY,
        PREVIOUS_VERIFICATION_TOKEN_EXPIRY,
        () -> successCallbackInvoked.set(true));

    verify(emailSender, never()).send(any());

    runAfterCommitCallbacks();

    verify(emailSender).send(emailMessage);
    verify(self, never())
        .restoreVerificationTokenExpiryWithErrorHandling(
            TEST_USER_ID, FAILED_ATTEMPT_EXPIRY, PREVIOUS_VERIFICATION_TOKEN_EXPIRY);
    assertTrue(successCallbackInvoked.get());
  }

  @Test
  void scheduleVerificationEmailRestoresExpiryWhenDeliveryIsSkipped() {
    VerificationEmailWorkflow verificationEmailWorkflow =
        new VerificationEmailWorkflow(authEmailFactory, emailSender, userRepository, self);
    EmailMessageDto emailMessage =
        new EmailMessageDto(TEST_EMAIL_LAURA, TEST_EMAIL_SUBJECT, TEST_EMAIL_HTML, TEST_EMAIL_TEXT);
    AtomicBoolean successCallbackInvoked = new AtomicBoolean(false);

    when(authEmailFactory.buildVerifyEmailMessage(
            TEST_EMAIL_LAURA, TEST_VERIFY_EMAIL_TOKEN, TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenReturn(EmailSendResult.SKIPPED_DAILY_QUOTA);

    TransactionSynchronizationManager.initSynchronization();

    verificationEmailWorkflow.scheduleVerificationEmail(
        TEST_USER_ID,
        TEST_EMAIL_LAURA,
        TEST_VERIFICATION_TOKEN,
        FAILED_ATTEMPT_EXPIRY,
        PREVIOUS_VERIFICATION_TOKEN_EXPIRY,
        () -> successCallbackInvoked.set(true));

    runAfterCommitCallbacks();

    verify(emailSender).send(emailMessage);
    verify(self)
        .restoreVerificationTokenExpiryWithErrorHandling(
            TEST_USER_ID, FAILED_ATTEMPT_EXPIRY, PREVIOUS_VERIFICATION_TOKEN_EXPIRY);
    assertFalse(successCallbackInvoked.get());
  }

  @Test
  void scheduleVerificationEmailRestoresExpiryAndRethrowsWhenSendFails() {
    VerificationEmailWorkflow verificationEmailWorkflow =
        new VerificationEmailWorkflow(authEmailFactory, emailSender, userRepository, self);
    EmailMessageDto emailMessage =
        new EmailMessageDto(TEST_EMAIL_LAURA, TEST_EMAIL_SUBJECT, TEST_EMAIL_HTML, TEST_EMAIL_TEXT);
    AtomicBoolean successCallbackInvoked = new AtomicBoolean(false);

    when(authEmailFactory.buildVerifyEmailMessage(
            TEST_EMAIL_LAURA, TEST_VERIFY_EMAIL_TOKEN, TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenThrow(new IllegalStateException("email send failed"));

    TransactionSynchronizationManager.initSynchronization();

    verificationEmailWorkflow.scheduleVerificationEmail(
        TEST_USER_ID,
        TEST_EMAIL_LAURA,
        TEST_VERIFICATION_TOKEN,
        FAILED_ATTEMPT_EXPIRY,
        PREVIOUS_VERIFICATION_TOKEN_EXPIRY,
        () -> successCallbackInvoked.set(true));

    assertThrows(IllegalStateException.class, this::runAfterCommitCallbacks);

    verify(emailSender).send(emailMessage);
    verify(self)
        .restoreVerificationTokenExpiryWithErrorHandling(
            TEST_USER_ID, FAILED_ATTEMPT_EXPIRY, PREVIOUS_VERIFICATION_TOKEN_EXPIRY);
    assertFalse(successCallbackInvoked.get());
  }

  @Test
  void restoreVerificationTokenExpiryWithErrorHandlingRestoresPreviousExpiryWhenCurrentMatches() {
    VerificationEmailWorkflow verificationEmailWorkflow =
        new VerificationEmailWorkflow(authEmailFactory, emailSender, userRepository, self);
    UserEntity user = new UserEntity();
    user.setVerificationTokenExpiresAt(FAILED_ATTEMPT_EXPIRY);

    when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

    verificationEmailWorkflow.restoreVerificationTokenExpiryWithErrorHandling(
        TEST_USER_ID, FAILED_ATTEMPT_EXPIRY, PREVIOUS_VERIFICATION_TOKEN_EXPIRY);

    assertEquals(PREVIOUS_VERIFICATION_TOKEN_EXPIRY, user.getVerificationTokenExpiresAt());
    verify(userRepository).save(user);
  }

  @Test
  void restoreVerificationTokenExpiryWithErrorHandlingSkipsUpdateWhenCurrentExpiryChanged() {
    VerificationEmailWorkflow verificationEmailWorkflow =
        new VerificationEmailWorkflow(authEmailFactory, emailSender, userRepository, self);
    UserEntity user = new UserEntity();
    user.setVerificationTokenExpiresAt(Instant.parse("2026-04-01T12:15:30Z"));

    when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

    verificationEmailWorkflow.restoreVerificationTokenExpiryWithErrorHandling(
        TEST_USER_ID, FAILED_ATTEMPT_EXPIRY, PREVIOUS_VERIFICATION_TOKEN_EXPIRY);

    assertEquals(Instant.parse("2026-04-01T12:15:30Z"), user.getVerificationTokenExpiresAt());
    verify(userRepository, never()).save(user);
  }

  private void runAfterCommitCallbacks() {
    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations()) {
      synchronization.afterCommit();
    }
  }
}
