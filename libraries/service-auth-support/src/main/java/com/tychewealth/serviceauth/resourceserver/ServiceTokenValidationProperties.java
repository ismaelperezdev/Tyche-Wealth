package com.tychewealth.serviceauth.resourceserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/** Issuer, JWKS endpoint, and audience trusted by this service resource server. */
@ConfigurationProperties(prefix = "app.service-auth.resource-server")
public record ServiceTokenValidationProperties(
    boolean enabled, String issuer, String jwkSetUri, String audience) {

  /** Validates the issuer, JWKS endpoint, and audience required for token validation. */
  public ServiceTokenValidationProperties {
    Assert.isTrue(enabled, "app.service-auth.resource-server.enabled must be true");
    Assert.hasText(issuer, "app.service-auth.resource-server.issuer must not be blank");
    Assert.hasText(jwkSetUri, "app.service-auth.resource-server.jwk-set-uri must not be blank");
    Assert.hasText(audience, "app.service-auth.resource-server.audience must not be blank");
  }
}
