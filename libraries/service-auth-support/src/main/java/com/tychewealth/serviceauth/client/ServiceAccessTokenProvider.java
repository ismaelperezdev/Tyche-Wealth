package com.tychewealth.serviceauth.client;

import static com.tychewealth.serviceauth.constants.LogConstants.ACCESS_TOKEN_ACTION;
import static com.tychewealth.serviceauth.constants.LogConstants.AUTHENTICATION_FAILURE;
import static com.tychewealth.serviceauth.constants.LogConstants.AUTHENTICATION_SUCCESS;
import static com.tychewealth.serviceauth.constants.LogConstants.NO_AUTHORIZED_CLIENT;
import static com.tychewealth.serviceauth.constants.LogConstants.SERVICE_AUTH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.util.Assert;

/** Obtains or reuses an OAuth2 access token for a configured client registration. */
public class ServiceAccessTokenProvider {

  private static final Logger log = LoggerFactory.getLogger(ServiceAccessTokenProvider.class);

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
    OAuth2AuthorizedClient authorizedClient;
    try {
      authorizedClient = authorizedClientManager.authorize(request);
    } catch (OAuth2AuthorizationException exception) {
      log.warn(
          AUTHENTICATION_FAILURE,
          SERVICE_AUTH,
          ACCESS_TOKEN_ACTION,
          exception.getError().getErrorCode());
      throw exception;
    }
    if (authorizedClient == null) {
      log.warn(NO_AUTHORIZED_CLIENT, SERVICE_AUTH, ACCESS_TOKEN_ACTION);
      throw new IllegalStateException(
          "No OAuth2 access token could be obtained for registration " + registrationId);
    }
    log.info(AUTHENTICATION_SUCCESS, SERVICE_AUTH, ACCESS_TOKEN_ACTION);
    return authorizedClient.getAccessToken().getTokenValue();
  }
}
