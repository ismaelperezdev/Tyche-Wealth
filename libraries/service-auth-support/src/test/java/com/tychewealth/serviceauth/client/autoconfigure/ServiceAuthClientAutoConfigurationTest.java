package com.tychewealth.serviceauth.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.serviceauth.client.ServiceAccessTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

class ServiceAuthClientAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  OAuth2ClientAutoConfiguration.class, ServiceAuthClientAutoConfiguration.class))
          .withPropertyValues(
              "spring.security.oauth2.client.registration.market-data.provider=market-data",
              "spring.security.oauth2.client.registration.market-data.client-id=portfolio-service",
              "spring.security.oauth2.client.registration.market-data.client-secret=test-secret",
              "spring.security.oauth2.client.registration.market-data.authorization-grant-type="
                  + "client_credentials",
              "spring.security.oauth2.client.registration.market-data.scope=market-prices:read",
              "spring.security.oauth2.client.provider.market-data.token-uri=https://user-service.test/oauth2/token");

  @Test
  void createsClientCredentialsProviderFromStandardSpringClientConfiguration() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager.class);
          assertThat(context).hasSingleBean(ServiceAccessTokenProvider.class);
        });
  }

  @Test
  void doesNotCreateProviderWhenClientConfigurationIsAbsent() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ServiceAuthClientAutoConfiguration.class))
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(OAuth2AuthorizedClientManager.class);
              assertThat(context).doesNotHaveBean(ServiceAccessTokenProvider.class);
            });
  }

  @Test
  void backsOffWhenClientClassesAreUnavailable() {
    new ApplicationContextRunner()
        .withClassLoader(new FilteredClassLoader("org.springframework.security.oauth2.client"))
        .withConfiguration(AutoConfigurations.of(ServiceAuthClientAutoConfiguration.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(ServiceAccessTokenProvider.class);
            });
  }
}
