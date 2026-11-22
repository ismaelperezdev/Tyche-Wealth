package com.tychewealth.serviceauth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class ServiceAccessTokenProviderTest {

  @Test
  void returnsAccessTokenFromAuthorizedClientManager() {
    var manager = mock(OAuth2AuthorizedClientManager.class);
    var authorizedClient = mock(OAuth2AuthorizedClient.class);
    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "access-token-value", null, null, null);
    when(manager.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(authorizedClient);
    when(authorizedClient.getAccessToken()).thenReturn(accessToken);

    var provider = new ServiceAccessTokenProvider(manager);

    assertEquals("access-token-value", provider.getAccessToken("market-data", "portfolio-service"));
  }

  @Test
  void failsClearlyWhenManagerCannotObtainAuthorizedClient() {
    var manager = mock(OAuth2AuthorizedClientManager.class);
    when(manager.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(null);

    var provider = new ServiceAccessTokenProvider(manager);

    assertThrows(
        IllegalStateException.class,
        () -> provider.getAccessToken("market-data", "portfolio-service"));
  }
}
