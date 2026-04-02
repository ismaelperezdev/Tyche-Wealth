package com.tychewealth.service.token;

import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_FAILURE;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_ACCESS_TOKEN_JTI;
import static com.tychewealth.constants.TestConstants.TEST_BEARER_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.monitoring.AuthMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenValidatorTest {

  @Mock private AccessTokenCodec accessTokenCodec;
  @Mock private TokenStateStore tokenStateStore;

  private SimpleMeterRegistry meterRegistry;
  private TokenValidator tokenValidator;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    AuthMetrics authMetrics = new AuthMetrics(meterRegistry);
    tokenValidator = new TokenValidator(accessTokenCodec, tokenStateStore, authMetrics);
  }

  @Test
  void validateAndExtractUserIdReturnsParsedUserIdWhenTokenIsNotRevoked() {
    when(tokenStateStore.extractBearerToken(TEST_BEARER_ACCESS_TOKEN))
        .thenReturn(TEST_ACCESS_TOKEN);
    when(accessTokenCodec.parseAccessToken(TEST_ACCESS_TOKEN))
        .thenReturn(new AccessTokenCodec.ParsedAccessToken(TEST_USER_ID, TEST_ACCESS_TOKEN_JTI));
    when(tokenStateStore.isAccessTokenRevoked(TEST_ACCESS_TOKEN_JTI)).thenReturn(false);

    Long userId = tokenValidator.validateAndExtractUserId(TEST_BEARER_ACCESS_TOKEN);

    assertEquals(TEST_USER_ID, userId);
  }

  @Test
  void validateAndExtractUserIdRejectsRevokedTokens() {
    when(tokenStateStore.extractBearerToken(TEST_BEARER_ACCESS_TOKEN))
        .thenReturn(TEST_ACCESS_TOKEN);
    when(accessTokenCodec.parseAccessToken(TEST_ACCESS_TOKEN))
        .thenReturn(new AccessTokenCodec.ParsedAccessToken(TEST_USER_ID, TEST_ACCESS_TOKEN_JTI));
    when(tokenStateStore.isAccessTokenRevoked(TEST_ACCESS_TOKEN_JTI)).thenReturn(true);

    assertThrows(
        AuthException.class,
        () -> tokenValidator.validateAndExtractUserId(TEST_BEARER_ACCESS_TOKEN));
  }

  @Test
  void extractBearerTokenDelegatesToStateStore() {
    when(tokenStateStore.extractBearerToken(TEST_BEARER_ACCESS_TOKEN))
        .thenReturn(TEST_ACCESS_TOKEN);

    String token = tokenValidator.extractBearerToken(TEST_BEARER_ACCESS_TOKEN);

    assertEquals(TEST_ACCESS_TOKEN, token);
    verify(tokenStateStore).extractBearerToken(TEST_BEARER_ACCESS_TOKEN);
  }

  @Test
  void validateRefreshTokenRequestRejectsMissingRefreshToken() {
    RefreshTokenRequestDto refreshTokenRequestDto = new RefreshTokenRequestDto(" ");

    assertThrows(
        AuthException.class,
        () -> tokenValidator.validateRefreshTokenRequest(refreshTokenRequestDto));

    assertEquals(1.0, meterRegistry.get(METRIC_AUTH_REFRESH_FAILURE).counter().count());
  }
}
