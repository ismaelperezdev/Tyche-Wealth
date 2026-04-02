package com.tychewealth.service.impl;

import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.LogConstants.REFRESH_TOKEN_ACTION;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_BEARER_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXISTING;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_COOKIE_NAME;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.MetricsTestHelper.counterValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.RegisteredUserResultDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.service.email.VerificationEmailWorkflow;
import com.tychewealth.service.helper.auth.AuthForgotPasswordHelper;
import com.tychewealth.service.helper.auth.AuthLoginHelper;
import com.tychewealth.service.helper.auth.AuthRefreshTokenHelper;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthResendVerificationEmailHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.auth.AuthVerifyEmailHelper;
import com.tychewealth.service.helper.auth.AuthVerifyLoginDeviceHelper;
import com.tychewealth.service.token.AccessTokenCodec;
import com.tychewealth.service.token.TokenStateStore;
import com.tychewealth.service.token.TokenValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  private static final Instant TEST_VERIFICATION_TOKEN_EXPIRES_AT =
      Instant.parse("2026-04-04T10:00:00Z");
  private static final String TEST_NEW_REFRESH_TOKEN = "new-refresh-token";

  @Mock private AuthValidationHelper authValidationHelper;
  @Mock private AuthRegisterHelper authRegisterHelper;
  @Mock private AuthLoginHelper authLoginHelper;
  @Mock private AuthResendVerificationEmailHelper authResendVerificationEmailHelper;
  @Mock private AuthVerifyEmailHelper authVerifyEmailHelper;
  @Mock private AuthVerifyLoginDeviceHelper authVerifyLoginDeviceHelper;
  @Mock private VerificationEmailWorkflow verificationEmailWorkflow;
  @Mock private TokenStateStore tokenStateStore;
  @Mock private AuthRefreshTokenHelper authRefreshTokenHelper;
  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private TokenValidator tokenValidator;
  @Mock private AuthForgotPasswordHelper authForgotPasswordHelper;
  private SimpleMeterRegistry meterRegistry;
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    authService =
        new AuthServiceImpl(
            authValidationHelper,
            authRegisterHelper,
            authLoginHelper,
            authResendVerificationEmailHelper,
            authVerifyEmailHelper,
            authVerifyLoginDeviceHelper,
            verificationEmailWorkflow,
            tokenStateStore,
            authRefreshTokenHelper,
            accessTokenCodec,
            tokenValidator,
            authForgotPasswordHelper,
            new AuthMetrics(meterRegistry));
  }

  @Test
  void verifyEmailDelegatesToHelper() {
    ResponseCookie cookie =
        ResponseCookie.from(TEST_TRUSTED_DEVICE_COOKIE_NAME, TEST_TRUSTED_DEVICE_TOKEN).build();
    when(authVerifyEmailHelper.verify(TEST_VERIFY_EMAIL_TOKEN)).thenReturn(cookie);

    ResponseCookie result = authService.verifyEmail(TEST_VERIFY_EMAIL_TOKEN);

    assertSame(cookie, result);
  }

  @Test
  void verifyLoginDeviceDelegatesToHelper() {
    ResponseCookie cookie =
        ResponseCookie.from(TEST_TRUSTED_DEVICE_COOKIE_NAME, TEST_TRUSTED_DEVICE_TOKEN).build();
    when(authVerifyLoginDeviceHelper.verify(TEST_VERIFY_EMAIL_TOKEN)).thenReturn(cookie);

    ResponseCookie result = authService.verifyLoginDevice(TEST_VERIFY_EMAIL_TOKEN);

    assertSame(cookie, result);
  }

  @Test
  void forgotPasswordDelegatesToHelper() {
    ForgotPasswordRequestDto requestDto = new ForgotPasswordRequestDto(TEST_EMAIL_LAURA);

    authService.forgotPassword(requestDto);

    verify(authForgotPasswordHelper).forgotPassword(requestDto);
  }

  @Test
  void registerValidatesCreatesUserSchedulesVerificationEmailAndReturnsResponse() {
    RegisterRequestDto requestDto =
        new RegisterRequestDto(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);
    UserResponseDto responseDto =
        new UserResponseDto(TEST_USER_ID, TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    AuthTokenDto verificationToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_VERIFY_EMAIL_TOKEN,
            TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    RegisteredUserResultDto registeredUser =
        new RegisteredUserResultDto(responseDto, verificationToken);
    ArgumentCaptor<Runnable> successCallbackCaptor = ArgumentCaptor.forClass(Runnable.class);

    when(authRegisterHelper.createUser(requestDto)).thenReturn(registeredUser);
    when(accessTokenCodec.extractExpiration(TEST_VERIFY_EMAIL_TOKEN))
        .thenReturn(TEST_VERIFICATION_TOKEN_EXPIRES_AT);

    UserResponseDto result = authService.register(requestDto);

    assertSame(responseDto, result);
    verify(authValidationHelper).validateRegisterRequest(requestDto);
    verify(verificationEmailWorkflow)
        .scheduleVerificationEmail(
            eq(TEST_USER_ID),
            eq(TEST_EMAIL_LAURA),
            eq(verificationToken),
            eq(TEST_VERIFICATION_TOKEN_EXPIRES_AT),
            eq(null),
            successCallbackCaptor.capture());

    successCallbackCaptor.getValue().run();

    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REGISTER_SUCCESS.metricName()));
  }

  @Test
  void registerThrowsMappedExceptionWhenPersistenceConflictHappens() {
    RegisterRequestDto requestDto =
        new RegisterRequestDto(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);
    DataIntegrityViolationException persistenceException =
        new DataIntegrityViolationException("duplicate");
    AuthException mappedException =
        new AuthException(ErrorDefinition.AUTH_REGISTRATION_CONFLICT, null, HttpStatus.CONFLICT);

    when(authRegisterHelper.createUser(requestDto)).thenThrow(persistenceException);
    when(authValidationHelper.validateRegisterPersistenceConflict(persistenceException))
        .thenReturn(mappedException);

    AuthException thrown =
        assertThrows(AuthException.class, () -> authService.register(requestDto));

    assertSame(mappedException, thrown);
  }

  @Test
  void resendVerificationEmailDelegatesToHelper() {
    ResendVerificationEmailRequestDto requestDto =
        new ResendVerificationEmailRequestDto(TEST_EMAIL_LAURA);

    authService.resendVerificationEmail(requestDto);

    verify(authResendVerificationEmailHelper).resendVerificationEmail(requestDto);
  }

  @Test
  void loginValidatesRequestAndDelegatesToLoginHelper() {
    LoginRequestDto requestDto = new LoginRequestDto(TEST_EMAIL_LAURA, TEST_PASSWORD_VALID);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    LoginResponseDto loginResponse =
        new LoginResponseDto(
            TOKEN_TYPE_BEARER, TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN_EXISTING, 900L, null);

    when(authValidationHelper.validateLoginRequest(requestDto)).thenReturn(user);
    when(authLoginHelper.login(user)).thenReturn(loginResponse);

    LoginResponseDto result = authService.login(requestDto);

    assertSame(loginResponse, result);
  }

  @Test
  void refreshValidatesRotatesTokensAndReturnsResponse() {
    RefreshTokenRequestDto requestDto = new RefreshTokenRequestDto(TEST_REFRESH_TOKEN_EXISTING);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    RefreshTokenEntity currentRefreshToken = new RefreshTokenEntity();
    currentRefreshToken.setUser(user);
    currentRefreshToken.setExpiresAt(TEST_VERIFICATION_TOKEN_EXPIRES_AT);
    AuthTokenDto accessToken =
        new AuthTokenDto(
            TOKEN_TYPE_BEARER,
            TEST_ACCESS_TOKEN,
            TEST_ACCESS_TOKEN_TTL_SECONDS,
            TEST_ACCESS_TOKEN_JTI);
    AuthRefreshTokenHelper.LinkedRefreshToken newRefreshToken =
        new AuthRefreshTokenHelper.LinkedRefreshToken(
            TEST_NEW_REFRESH_TOKEN, TEST_VERIFICATION_TOKEN_EXPIRES_AT);

    when(authRefreshTokenHelper.validateRefreshToken(TEST_REFRESH_TOKEN_EXISTING))
        .thenReturn(currentRefreshToken);
    when(accessTokenCodec.generateToken(user, AccessTokenType.ACCESS)).thenReturn(accessToken);
    when(authRefreshTokenHelper.saveToken(user, TEST_ACCESS_TOKEN_JTI, REFRESH_TOKEN_ACTION))
        .thenReturn(newRefreshToken);

    RefreshTokenResponseDto result = authService.refresh(requestDto);

    verify(tokenValidator).validateRefreshTokenRequest(requestDto);
    verify(tokenStateStore).unlinkRefreshToken(TEST_REFRESH_TOKEN_EXISTING);
    assertEquals(TOKEN_TYPE_BEARER, result.getTokenType());
    assertEquals(TEST_ACCESS_TOKEN, result.getAccessToken());
    assertEquals(TEST_ACCESS_TOKEN_TTL_SECONDS, result.getExpiresIn());
    assertEquals(TEST_NEW_REFRESH_TOKEN, result.getRefreshToken());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REFRESH_SUCCESS.metricName()));
  }

  @Test
  void logoutRevokesTokensAndUnlinksRefreshToken() {
    RefreshTokenRequestDto requestDto = new RefreshTokenRequestDto(TEST_REFRESH_TOKEN_EXISTING);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();
    refreshToken.setUser(user);
    refreshToken.setExpiresAt(TEST_VERIFICATION_TOKEN_EXPIRES_AT);

    when(authRefreshTokenHelper.validateRefreshToken(TEST_REFRESH_TOKEN_EXISTING))
        .thenReturn(refreshToken);
    when(tokenStateStore.findAccessTokenJtiByRefreshToken(TEST_REFRESH_TOKEN_EXISTING))
        .thenReturn(Optional.of(TEST_ACCESS_TOKEN_JTI));

    authService.logout(TEST_BEARER_ACCESS_TOKEN, requestDto);

    verify(tokenValidator).validateRefreshTokenRequest(requestDto);
    verify(tokenStateStore).revokeAccessTokenIfPresent(TEST_BEARER_ACCESS_TOKEN);
    verify(tokenStateStore)
        .revokeAccessToken(TEST_ACCESS_TOKEN_JTI, TEST_VERIFICATION_TOKEN_EXPIRES_AT);
    verify(tokenStateStore).unlinkRefreshToken(TEST_REFRESH_TOKEN_EXISTING);
  }

  @Test
  void logoutStillUnlinksRefreshTokenWhenNoLinkedAccessTokenExists() {
    RefreshTokenRequestDto requestDto = new RefreshTokenRequestDto(TEST_REFRESH_TOKEN_EXISTING);
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();
    refreshToken.setUser(user);

    when(authRefreshTokenHelper.validateRefreshToken(TEST_REFRESH_TOKEN_EXISTING))
        .thenReturn(refreshToken);
    when(tokenStateStore.findAccessTokenJtiByRefreshToken(TEST_REFRESH_TOKEN_EXISTING))
        .thenReturn(Optional.empty());

    authService.logout(TEST_BEARER_ACCESS_TOKEN, requestDto);

    verify(tokenStateStore, never()).revokeAccessToken(eq(TEST_ACCESS_TOKEN_JTI), any());
    verify(tokenStateStore).unlinkRefreshToken(TEST_REFRESH_TOKEN_EXISTING);
  }
}
