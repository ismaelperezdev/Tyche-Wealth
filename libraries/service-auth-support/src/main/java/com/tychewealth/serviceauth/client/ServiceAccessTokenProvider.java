package com.tychewealth.serviceauth.client;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.util.Assert;

/** Obtains or reuses an OAuth2 access token for a configured client registration. */
public class ServiceAccessTokenProvider {

  private final OAuth2AuthorizedClientManager authorizedClientManager;

  public ServiceAccessTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
    Assert.notNull(authorizedClientManager, "authorizedClientManager must not be null");
    this.authorizedClientManager = authorizedClientManager;
  }

  /** Returns the access token value, renewing it through the manager when it has expired. */
  public String getAccessToken(String registrationId, String principalName) {
    Assert.hasText(registrationId, "registrationId must not be blank");
    Assert.hasText(principalName, "principalName must not be blank");

    OAuth2AuthorizeRequest request =
        OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
            .principal(principalName)
            .build();
    OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
    if (authorizedClient == null) {
      throw new IllegalStateException(
          "No OAuth2 access token could be obtained for registration " + registrationId);
    }
    return authorizedClient.getAccessToken().getTokenValue();
  }
}
