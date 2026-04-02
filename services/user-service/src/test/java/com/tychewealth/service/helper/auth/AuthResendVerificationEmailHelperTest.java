package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.email.VerificationEmailWorkflow;
import com.tychewealth.service.token.AccessTokenCodec;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthResendVerificationEmailHelperTest {

  private static final Instant TEST_PREVIOUS_VERIFICATION_TOKEN_EXPIRES_AT =
      Instant.parse("2026-04-02T10:00:00Z");
  private static final Instant TEST_FAILED_ATTEMPT_EXPIRY = Instant.parse("2026-04-03T10:00:00Z");

  @Mock private UserRepository userRepository;
  @Mock private AuthValidationHelper authValidationHelper;
  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private VerificationEmailWorkflow verificationEmailWorkflow;

  @InjectMocks private AuthResendVerificationEmailHelper authResendVerificationEmailHelper;

  @Test
  void resendVerificationEmailReturnsSilentlyWhenUserDoesNotExist() {
    ResendVerificationEmailRequestDto requestDto =
        new ResendVerificationEmailRequestDto(TEST_EMAIL_LAURA);

    when(userRepository.findByEmailAndDeletedAtIsNullForUpdate(TEST_EMAIL_LAURA))
        .thenReturn(Optional.empty());

    authResendVerificationEmailHelper.resendVerificationEmail(requestDto);

    verify(userRepository).findByEmailAndDeletedAtIsNullForUpdate(TEST_EMAIL_LAURA);
    verifyNoInteractions(authValidationHelper, accessTokenCodec, verificationEmailWorkflow);
  }

  @Test
  void resendVerificationEmailReturnsSilentlyWhenUserCannotResend() {
    ResendVerificationEmailRequestDto requestDto =
        new ResendVerificationEmailRequestDto(TEST_EMAIL_LAURA);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TEST_USER_ID);

    when(userRepository.findByEmailAndDeletedAtIsNullForUpdate(TEST_EMAIL_LAURA))
        .thenReturn(Optional.of(user));
    when(authValidationHelper.canResendVerificationEmail(user)).thenReturn(false);

    authResendVerificationEmailHelper.resendVerificationEmail(requestDto);

    verify(authValidationHelper).canResendVerificationEmail(user);
    verify(accessTokenCodec, never()).generateToken(user, AccessTokenType.VERIFY_EMAIL);
    verifyNoInteractions(verificationEmailWorkflow);
  }

  @Test
  void resendVerificationEmailSchedulesWorkflowAndUpdatesExpiryWhenUserCanResend() {
    ResendVerificationEmailRequestDto requestDto =
        new ResendVerificationEmailRequestDto(TEST_EMAIL_LAURA);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TEST_USER_ID);
    user.setVerificationTokenExpiresAt(TEST_PREVIOUS_VERIFICATION_TOKEN_EXPIRES_AT);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_EMAIL_TOKEN,
            TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);

    when(userRepository.findByEmailAndDeletedAtIsNullForUpdate(TEST_EMAIL_LAURA))
        .thenReturn(Optional.of(user));
    when(authValidationHelper.canResendVerificationEmail(user)).thenReturn(true);
    when(accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_EMAIL))
        .thenReturn(verificationToken);
    when(accessTokenCodec.extractExpiration(TEST_VERIFY_EMAIL_TOKEN))
        .thenReturn(TEST_FAILED_ATTEMPT_EXPIRY);

    authResendVerificationEmailHelper.resendVerificationEmail(requestDto);

    assertEquals(TEST_FAILED_ATTEMPT_EXPIRY, user.getVerificationTokenExpiresAt());
    verify(accessTokenCodec).generateToken(user, AccessTokenType.VERIFY_EMAIL);
    verify(accessTokenCodec).extractExpiration(TEST_VERIFY_EMAIL_TOKEN);
    verify(verificationEmailWorkflow)
        .scheduleVerificationEmail(
            eq(TEST_USER_ID),
            eq(TEST_EMAIL_LAURA),
            eq(verificationToken),
            eq(TEST_FAILED_ATTEMPT_EXPIRY),
            eq(TEST_PREVIOUS_VERIFICATION_TOKEN_EXPIRES_AT),
            any(Runnable.class));
  }
}
