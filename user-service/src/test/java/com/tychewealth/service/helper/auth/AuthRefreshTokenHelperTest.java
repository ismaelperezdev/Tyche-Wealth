package com.tychewealth.service.helper.auth;

import static com.tychewealth.constants.LogConstants.LOGIN_ACTION;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_RATE_LIMIT_STORE_UNAVAILABLE;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXISTING;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_PEPPER;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.MetricsTestHelper.counterValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AuthMetricEnum;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.service.token.TokenStateStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AuthRefreshTokenHelperTest {

  private static final long TEST_REFRESH_TOKEN_TTL_SECONDS = 1209600L;

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TokenStateStore tokenStateStore;

  private SimpleMeterRegistry meterRegistry;
  private AuthRefreshTokenHelper authRefreshTokenHelper;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    authRefreshTokenHelper =
        new AuthRefreshTokenHelper(
            refreshTokenRepository,
            tokenStateStore,
            new AuthMetrics(meterRegistry),
            TEST_REFRESH_TOKEN_TTL_SECONDS,
            TEST_REFRESH_TOKEN_PEPPER);
  }

  @Test
  void saveTokenPersistsRefreshTokenLinksStateAndReturnsIssuedToken() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    ArgumentCaptor<RefreshTokenEntity> entityCaptor =
        ArgumentCaptor.forClass(RefreshTokenEntity.class);
    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);

    AuthRefreshTokenHelper.LinkedRefreshToken result =
        authRefreshTokenHelper.saveToken(user, TEST_ACCESS_TOKEN_JTI, LOGIN_ACTION);

    assertNotNull(result.token());
    assertNotNull(result.expiresAt());
    assertTrue(result.expiresAt().isAfter(Instant.now()));

    verify(refreshTokenRepository).save(entityCaptor.capture());
    verify(tokenStateStore)
        .linkRefreshTokenToAccessToken(
            tokenCaptor.capture(),
            org.mockito.ArgumentMatchers.eq(TEST_ACCESS_TOKEN_JTI),
            expiresAtCaptor.capture());

    RefreshTokenEntity persisted = entityCaptor.getValue();
    assertSame(user, persisted.getUser());
    assertEquals(authRefreshTokenHelper.hashRefreshToken(result.token()), persisted.getToken());
    assertEquals(result.expiresAt(), persisted.getExpiresAt());
    assertEquals(false, persisted.isRevoked());
    assertEquals(result.token(), tokenCaptor.getValue());
    assertEquals(result.expiresAt(), expiresAtCaptor.getValue());
    assertEquals(
        1.0, counterValue(meterRegistry, AuthMetricEnum.REFRESH_TOKEN_ISSUED.metricName()));
  }

  @Test
  void saveTokenRollsBackAndThrowsWhenLinkingStateFailsRepeatedly() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setId(TEST_USER_ID);
    ArgumentCaptor<RefreshTokenEntity> entityCaptor =
        ArgumentCaptor.forClass(RefreshTokenEntity.class);

    org.mockito.Mockito.doThrow(new IllegalStateException(TEST_RATE_LIMIT_STORE_UNAVAILABLE))
        .when(tokenStateStore)
        .linkRefreshTokenToAccessToken(anyString(), anyString(), any(Instant.class));

    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authRefreshTokenHelper.saveToken(user, TEST_ACCESS_TOKEN_JTI, LOGIN_ACTION));

    verify(refreshTokenRepository).save(entityCaptor.capture());
    verify(refreshTokenRepository).deleteByToken(entityCaptor.getValue().getToken());
    assertEquals(ErrorDefinition.GENERIC_INTERNAL_ERROR, exception.getErrorDefinition());
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    assertEquals(
        1.0, counterValue(meterRegistry, AuthMetricEnum.TOKEN_STATE_UNAVAILABLE.metricName()));
  }

  @Test
  void revokeActiveTokensByUserIdReturnsRevokedCountAndIncrementsMetric() {
    when(refreshTokenRepository.revokeActiveTokensByUserId(
            org.mockito.ArgumentMatchers.eq(TEST_USER_ID), any(Instant.class)))
        .thenReturn(3);

    int result = authRefreshTokenHelper.revokeActiveTokensByUserId(TEST_USER_ID);

    assertEquals(3, result);
    assertEquals(
        3.0, counterValue(meterRegistry, AuthMetricEnum.REFRESH_TOKEN_REVOKED.metricName()));
  }

  @Test
  void validateRefreshTokenReturnsEntityWhenTokenIsActive() {
    RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
    String hashedToken = authRefreshTokenHelper.hashRefreshToken(TEST_REFRESH_TOKEN_EXISTING);

    when(refreshTokenRepository.revokeTokenIfActive(
            org.mockito.ArgumentMatchers.eq(hashedToken), any(Instant.class)))
        .thenReturn(1);
    when(refreshTokenRepository.findByToken(hashedToken))
        .thenReturn(Optional.of(refreshTokenEntity));

    RefreshTokenEntity result =
        authRefreshTokenHelper.validateRefreshToken(TEST_REFRESH_TOKEN_EXISTING);

    assertSame(refreshTokenEntity, result);
    assertEquals(
        1.0, counterValue(meterRegistry, AuthMetricEnum.REFRESH_TOKEN_REVOKED.metricName()));
  }

  @Test
  void validateRefreshTokenThrowsUnauthorizedWhenTokenIsInvalid() {
    String hashedToken = authRefreshTokenHelper.hashRefreshToken(TEST_REFRESH_TOKEN_EXISTING);
    when(refreshTokenRepository.revokeTokenIfActive(
            org.mockito.ArgumentMatchers.eq(hashedToken), any(Instant.class)))
        .thenReturn(0);

    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authRefreshTokenHelper.validateRefreshToken(TEST_REFRESH_TOKEN_EXISTING));

    assertEquals(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID, exception.getErrorDefinition());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    assertEquals(1.0, counterValue(meterRegistry, AuthMetricEnum.REFRESH_FAILURE.metricName()));
    assertEquals(
        0.0, counterValue(meterRegistry, AuthMetricEnum.REFRESH_TOKEN_REVOKED.metricName()));
  }
}
