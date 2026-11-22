package com.tychewealth.serviceauth.resourceserver.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.serviceauth.resourceserver.ServiceTokenValidationProperties;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class ServiceAuthResourceServerAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ServiceAuthResourceServerAutoConfiguration.class,
                  OAuth2ResourceServerAutoConfiguration.class))
          .withPropertyValues(
              "app.service-auth.resource-server.enabled=true",
              "app.service-auth.resource-server.issuer=https://user-service.test",
              "app.service-auth.resource-server.jwk-set-uri=https://user-service.test/oauth2/jwks",
              "app.service-auth.resource-server.audience=market-data-service");

  @Test
  void createsJwtDecoderWhenIssuerJwksAndAudienceAreConfigured() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(JwtDecoder.class));
  }

  @Test
  void doesNotCreateDecoderWhenTrustConfigurationIsAbsent() {
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ServiceAuthResourceServerAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
  }

  @Test
  void backsOffWhenResourceServerClassesAreUnavailable() {
    new ApplicationContextRunner()
        .withClassLoader(new FilteredClassLoader("org.springframework.security.oauth2.jwt"))
        .withConfiguration(AutoConfigurations.of(ServiceAuthResourceServerAutoConfiguration.class))
        .withPropertyValues("app.service-auth.resource-server.enabled=true")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(JwtDecoder.class);
            });
  }

  @Test
  void failsStartupWhenEnabledButRequiredTrustSettingsAreMissing() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ServiceAuthResourceServerAutoConfiguration.class))
        .withPropertyValues("app.service-auth.resource-server.enabled=true")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void requiresConfiguredAudienceAndExpirationClaim() {
    var properties =
        new ServiceTokenValidationProperties(
            true,
            "https://user-service.test",
            "https://user-service.test/oauth2/jwks",
            "market-data-service");
    var validator = ServiceAuthResourceServerAutoConfiguration.jwtValidator(properties);
    Instant now = Instant.now();
    Jwt validJwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("token_type", "service")
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build();
    Jwt missingExpirationJwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("token_type", "service")
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .issuedAt(now)
            .build();
    Jwt wrongAudienceJwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("token_type", "service")
            .issuer(properties.issuer())
            .audience(List.of("another-service"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build();
    Jwt missingAudienceJwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("token_type", "service")
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build();

    assertThat(validator.validate(validJwt).hasErrors()).isFalse();
    assertThat(validator.validate(missingExpirationJwt).hasErrors()).isTrue();
    assertThat(validator.validate(wrongAudienceJwt).hasErrors()).isTrue();
    assertThat(validator.validate(missingAudienceJwt).hasErrors()).isTrue();
  }
}
