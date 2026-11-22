package com.tychewealth.serviceauth.resourceserver.autoconfigure;

import com.tychewealth.serviceauth.resourceserver.ServiceTokenValidationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Configures shared JWT validation for service resource servers. */
@AutoConfiguration(
    beforeName =
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet."
            + "OAuth2ResourceServerAutoConfiguration")
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.JwtDecoder")
@ConditionalOnProperty(
    prefix = "app.service-auth.resource-server",
    name = "enabled",
    havingValue = "true")
@EnableConfigurationProperties(ServiceTokenValidationProperties.class)
public class ServiceAuthResourceServerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  JwtDecoder serviceTokenJwtDecoder(ServiceTokenValidationProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
    decoder.setJwtValidator(jwtValidator(properties));
    return decoder;
  }

  static OAuth2TokenValidator<Jwt> jwtValidator(ServiceTokenValidationProperties properties) {
    return new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(properties.issuer()),
        jwt ->
            jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_TOKEN,
                        "Token does not contain the required expiration claim",
                        null)),
        jwt ->
            jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_TOKEN,
                        "Token does not contain the configured audience",
                        null)));
  }
}
