package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.LogConstants.LOGIN_ACTION;
import static com.tychewealth.constants.RedisConstants.AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_HTML_BODY;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_SUBJECT_VERIFY;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_TEXT_BODY;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXISTING;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_LOGIN_DEVICE_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.MetricsTestHelper.counterValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.enums.EmailSendResult;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.exception.EmailException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.RateLimitStore;
import com.tychewealth.service.email.AuthEmailFactory;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AuthLoginHelperTest {

  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private AuthRefreshTokenHelper refreshTokenHelper;
  @Mock private TrustedDeviceManager trustedDeviceManager;
  @Mock private AuthEmailFactory authEmailFactory;
  @Mock private EmailSender emailSender;
  @Mock private RateLimitStore rateLimitStore;
  @Mock private UserMapper userMapper;

  private SimpleMeterRegistry meterRegistry;
  private AuthLoginHelper authLoginHelper;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    authLoginHelper =
        new AuthLoginHelper(
            accessTokenCodec,
            refreshTokenHelper,
            trustedDeviceManager,
            authEmailFactory,
            emailSender,
            rateLimitStore,
            userMapper,
            new AuthMetrics(meterRegistry));
  }

  @Test
  void loginReturnsTokensAndUserWhenCurrentDeviceIsTrusted() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    UserResponseDto responseDto =
        new UserResponseDto(TEST_USER_ID, TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    AuthTokenDto accessToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_ACCESS_TOKEN,
            TEST_ACCESS_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    AuthRefreshTokenHelper.LinkedRefreshToken refreshToken =
        new AuthRefreshTokenHelper.LinkedRefreshToken(TEST_REFRESH_TOKEN_EXISTING, null);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(true);
    when(userMapper.toDto(user)).thenReturn(responseDto);
    when(accessTokenCodec.generateToken(user, AccessTokenType.ACCESS)).thenReturn(accessToken);
    when(refreshTokenHelper.saveToken(user, TEST_ACCESS_TOKEN_JTI, LOGIN_ACTION))
        .thenReturn(refreshToken);

    LoginResponseDto result = authLoginHelper.login(user);

    assertEquals(TOKEN_TYPE_BEARER, result.getTokenType());
    assertEquals(TEST_ACCESS_TOKEN, result.getAccessToken());
    assertEquals(TEST_REFRESH_TOKEN_EXISTING, result.getRefreshToken());
    assertEquals(TEST_ACCESS_TOKEN_TTL_SECONDS, result.getExpiresIn());
    assertSame(responseDto, result.getUser());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.LOGIN_SUCCESS.metricName()));

    verify(refreshTokenHelper).revokeActiveTokensByUserId(TEST_USER_ID);
    verify(refreshTokenHelper).saveToken(user, TEST_ACCESS_TOKEN_JTI, LOGIN_ACTION);
    verifyNoInteractions(authEmailFactory, emailSender, rateLimitStore);
  }

  @Test
  void loginThrowsConflictWhenDeviceIsUntrustedAndTrustedDeviceLimitIsReached() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(true);

    AuthException exception = assertThrows(AuthException.class, () -> authLoginHelper.login(user));

    assertEquals(ErrorDefinition.AUTH_TRUSTED_DEVICE_LIMIT_REACHED, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    verifyNoInteractions(
        accessTokenCodec, refreshTokenHelper, authEmailFactory, emailSender, rateLimitStore);
  }

  @Test
  void loginSendsVerificationEmailAndThrowsForbiddenWhenDeviceIsUntrusted() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(
            TEST_EMAIL_LAURA,
            TEST_EMAIL_SUBJECT_VERIFY,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(false);
    when(rateLimitStore.increment(
            AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
            String.valueOf(TEST_USER_ID),
            java.time.Duration.ofDays(1)))
        .thenReturn(1L);
    when(accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE))
        .thenReturn(verificationToken);
    when(authEmailFactory.buildVerifyLoginDeviceEmailMessage(
            TEST_EMAIL_LAURA,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenReturn(EmailSendResult.DELIVERED);

    AuthException exception = assertThrows(AuthException.class, () -> authLoginHelper.login(user));

    assertEquals(
        ErrorDefinition.AUTH_LOGIN_DEVICE_VERIFICATION_REQUIRED, exception.getErrorDefinition());
    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    verify(emailSender).send(emailMessage);
    verify(refreshTokenHelper, never()).revokeActiveTokensByUserId(TEST_USER_ID);
  }

  @Test
  void loginThrowsForbiddenWithoutSendingEmailWhenCooldownIsActive() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(false);
    when(rateLimitStore.increment(
            AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
            String.valueOf(TEST_USER_ID),
            java.time.Duration.ofDays(1)))
        .thenReturn(2L);

    AuthException exception = assertThrows(AuthException.class, () -> authLoginHelper.login(user));

    assertEquals(
        ErrorDefinition.AUTH_LOGIN_DEVICE_VERIFICATION_REQUIRED, exception.getErrorDefinition());
    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    verifyNoInteractions(accessTokenCodec, authEmailFactory, emailSender);
    verify(refreshTokenHelper, never()).revokeActiveTokensByUserId(TEST_USER_ID);
  }

  @Test
  void loginAllowsVerificationEmailWhenCooldownStoreIsUnavailable() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(
            TEST_EMAIL_LAURA,
            TEST_EMAIL_SUBJECT_VERIFY,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(false);
    when(rateLimitStore.increment(
            AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
            String.valueOf(TEST_USER_ID),
            java.time.Duration.ofDays(1)))
        .thenThrow(new IllegalStateException("redis unavailable"));
    when(accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE))
        .thenReturn(verificationToken);
    when(authEmailFactory.buildVerifyLoginDeviceEmailMessage(
            TEST_EMAIL_LAURA,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenReturn(EmailSendResult.DELIVERED);

    AuthException exception = assertThrows(AuthException.class, () -> authLoginHelper.login(user));

    assertEquals(
        ErrorDefinition.AUTH_LOGIN_DEVICE_VERIFICATION_REQUIRED, exception.getErrorDefinition());
    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    verify(emailSender).send(emailMessage);
    verify(rateLimitStore, never())
        .clear(AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE, String.valueOf(TEST_USER_ID));
  }

  @Test
  void loginThrowsEmailExceptionAndRollsBackCooldownWhenDeliveryIsSkippedByQuota() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(
            TEST_EMAIL_LAURA,
            TEST_EMAIL_SUBJECT_VERIFY,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(false);
    when(rateLimitStore.increment(
            AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
            String.valueOf(TEST_USER_ID),
            java.time.Duration.ofDays(1)))
        .thenReturn(1L);
    when(accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE))
        .thenReturn(verificationToken);
    when(authEmailFactory.buildVerifyLoginDeviceEmailMessage(
            TEST_EMAIL_LAURA,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenReturn(EmailSendResult.SKIPPED_DAILY_QUOTA);

    EmailException exception =
        assertThrows(EmailException.class, () -> authLoginHelper.login(user));

    assertEquals(ErrorDefinition.EMAIL_DELIVERY_FAILED, exception.getErrorDefinition());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    verify(rateLimitStore)
        .clear(AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE, String.valueOf(TEST_USER_ID));
    verify(refreshTokenHelper, never()).revokeActiveTokensByUserId(TEST_USER_ID);
  }

  @Test
  void loginRethrowsOriginalSendFailureWhenCooldownRollbackAlsoFails() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(
            TEST_EMAIL_LAURA,
            TEST_EMAIL_SUBJECT_VERIFY,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY);
    EmailException sendException =
        EmailException.of(
            ErrorDefinition.EMAIL_DELIVERY_FAILED, null, HttpStatus.INTERNAL_SERVER_ERROR);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(false);
    when(rateLimitStore.increment(
            AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
            String.valueOf(TEST_USER_ID),
            java.time.Duration.ofDays(1)))
        .thenReturn(1L);
    when(accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE))
        .thenReturn(verificationToken);
    when(authEmailFactory.buildVerifyLoginDeviceEmailMessage(
            TEST_EMAIL_LAURA,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenThrow(sendException);
    org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
        .when(rateLimitStore)
        .clear(AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE, String.valueOf(TEST_USER_ID));

    EmailException exception =
        assertThrows(EmailException.class, () -> authLoginHelper.login(user));

    assertSame(sendException, exception);
    verify(rateLimitStore)
        .clear(AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE, String.valueOf(TEST_USER_ID));
    verify(refreshTokenHelper, never()).revokeActiveTokensByUserId(TEST_USER_ID);
  }

  @Test
  void loginRethrowsSendFailureAndRollsBackCooldown() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    EmailMessageDto emailMessage =
        new EmailMessageDto(
            TEST_EMAIL_LAURA,
            TEST_EMAIL_SUBJECT_VERIFY,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY);
    EmailException sendException =
        EmailException.of(
            ErrorDefinition.EMAIL_DELIVERY_FAILED, null, HttpStatus.INTERNAL_SERVER_ERROR);

    when(trustedDeviceManager.isTrustedCurrentDevice(user)).thenReturn(false);
    when(trustedDeviceManager.hasReachedTrustedDeviceLimit(TEST_USER_ID)).thenReturn(false);
    when(rateLimitStore.increment(
            AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE,
            String.valueOf(TEST_USER_ID),
            java.time.Duration.ofDays(1)))
        .thenReturn(1L);
    when(accessTokenCodec.generateToken(user, AccessTokenType.VERIFY_LOGIN_DEVICE))
        .thenReturn(verificationToken);
    when(authEmailFactory.buildVerifyLoginDeviceEmailMessage(
            TEST_EMAIL_LAURA,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS))
        .thenReturn(emailMessage);
    when(emailSender.send(emailMessage)).thenThrow(sendException);

    EmailException exception =
        assertThrows(EmailException.class, () -> authLoginHelper.login(user));

    assertSame(sendException, exception);
    verify(rateLimitStore)
        .clear(AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE, String.valueOf(TEST_USER_ID));
    verify(refreshTokenHelper, never()).revokeActiveTokensByUserId(TEST_USER_ID);
  }
}
