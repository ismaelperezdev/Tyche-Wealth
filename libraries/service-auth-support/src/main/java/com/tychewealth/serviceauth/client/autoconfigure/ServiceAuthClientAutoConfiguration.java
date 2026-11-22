package com.tychewealth.serviceauth.client.autoconfigure;

import com.tychewealth.serviceauth.client.ServiceAccessTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/** Auto-configures client-credentials token acquisition for service-to-service calls. */
@AutoConfiguration(
    afterName =
        "org.springframework.boot.autoconfigure.security.oauth2.client."
            + "OAuth2ClientAutoConfiguration")
@ConditionalOnClass(
    name = "org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager")
public class ServiceAuthClientAutoConfiguration {

  @Bean
  @ConditionalOnBean({ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class})
  @ConditionalOnMissingBean(OAuth2AuthorizedClientManager.class)
  OAuth2AuthorizedClientManager serviceAuthAuthorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService) {
    var manager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);
    manager.setAuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
    return manager;
  }

  @Bean
  @ConditionalOnBean(OAuth2AuthorizedClientManager.class)
  @ConditionalOnMissingBean
  ServiceAccessTokenProvider serviceAccessTokenProvider(
      OAuth2AuthorizedClientManager authorizedClientManager) {
    return new ServiceAccessTokenProvider(authorizedClientManager);
  }
}
