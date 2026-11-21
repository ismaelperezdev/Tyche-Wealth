package com.tychewealth.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.tychewealth.dto.security.InternalServiceTokenPropertiesDto;
import com.tychewealth.dto.security.InternalServiceTokenPropertiesDto.ServiceClientProperties;
import com.tychewealth.dto.security.InternalServiceTokenSigningProperties;
import com.tychewealth.dto.security.InternalServiceTokenSigningProperties.PreviousPublicKeyProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

class InternalServiceTokenSigningKeyConfigurationTest {

  @TempDir Path temporaryDirectory;

  @Test
  void loadsConfiguredPkcs8PemAndUsesConfiguredKeyId() throws Exception {
    var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    var activeKeyPair = keyPairGenerator.generateKeyPair();
    var previousKeyPair = keyPairGenerator.generateKeyPair();
    Path privateKeyFile = temporaryDirectory.resolve("service-token-private.pem");
    Path previousPublicKeyFile = temporaryDirectory.resolve("previous-service-token-public.pem");
    Files.writeString(
        privateKeyFile, toPem("PRIVATE KEY", activeKeyPair.getPrivate().getEncoded()));
    Files.writeString(
        previousPublicKeyFile, toPem("PUBLIC KEY", previousKeyPair.getPublic().getEncoded()));

    var properties =
        new InternalServiceTokenSigningProperties(
            "https://user-service.test",
            "active-signing-key",
            privateKeyFile.toUri().toString(),
            List.of(
                new PreviousPublicKeyProperties(
                    "previous-signing-key", previousPublicKeyFile.toUri().toString())));
    var jwkSource =
        new InternalServiceTokenServerConfig()
            .serviceTokenJwkSource(properties, new DefaultResourceLoader());

    var keys =
        jwkSource.get(
            new JWKSelector(new JWKMatcher.Builder().keyID("active-signing-key").build()),
            (SecurityContext) null);
    assertEquals(1, keys.size());
    RSAKey jwk = (RSAKey) keys.getFirst();
    assertNotNull(jwk.toPublicJWK());
    assertFalse(jwk.toPublicJWK().isPrivate());
    assertEquals(activeKeyPair.getPublic(), jwk.toRSAPublicKey());

    RSAKey previousJwk =
        (RSAKey)
            jwkSource
                .get(
                    new JWKSelector(new JWKMatcher.Builder().keyID("previous-signing-key").build()),
                    (SecurityContext) null)
                .getFirst();
    assertFalse(previousJwk.isPrivate());
    assertEquals(previousKeyPair.getPublic(), previousJwk.toRSAPublicKey());

    var clientProperties =
        new InternalServiceTokenPropertiesDto(
            Set.of("market-data-service"),
            List.of(
                new ServiceClientProperties(
                    "portfolio-service",
                    "test-secret",
                    Set.of("market-prices:read"),
                    Set.of("market-data-service"),
                    300)));
    var registeredClient =
        RegisteredClient.withId("portfolio-registration")
            .clientId("portfolio-service")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .clientSettings(
                ClientSettings.builder()
                    .setting(
                        InternalServiceTokenServerConfig.SERVICE_TOKEN_AUDIENCES_SETTING,
                        List.of("market-data-service"))
                    .build())
            .build();
    var now = Instant.now();
    var context =
        JwtEncodingContext.with(
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256),
                JwtClaimsSet.builder()
                    .subject("portfolio-service")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(60)))
            .registeredClient(registeredClient)
            .principal(new TestingAuthenticationToken("portfolio-service", "test"))
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .build();
    new InternalServiceTokenServerConfig()
        .serviceTokenClaimsCustomizer(clientProperties, properties)
        .customize(context);

    var token =
        new NimbusJwtEncoder(jwkSource)
            .encode(
                JwtEncoderParameters.from(
                    context.getJwsHeader().build(), context.getClaims().build()));
    var signedJwt = SignedJWT.parse(token.getTokenValue());
    assertEquals("active-signing-key", signedJwt.getHeader().getKeyID());
    assertTrue(signedJwt.verify(new RSASSAVerifier((RSAPublicKey) activeKeyPair.getPublic())));
  }

  private String toPem(String keyType, byte[] encodedKey) {
    return "-----BEGIN "
        + keyType
        + "-----\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encodedKey)
        + "\n-----END "
        + keyType
        + "-----\n";
  }
}
