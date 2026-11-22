package com.tychewealth.serviceauth.resourceserver.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.tychewealth.serviceauth.ServiceAuthTestApplication;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    classes = ServiceAuthTestApplication.class,
    properties = {
      "app.service-auth.resource-server.enabled=true",
      "app.service-auth.resource-server.issuer=https://user-service.test",
      "app.service-auth.resource-server.audience=market-data-service"
    })
class ServiceAuthJwtDecoderIntegrationTest {

  private static final String ISSUER = "https://user-service.test";
  private static final String AUDIENCE = "market-data-service";
  private static RSAKey signingKey;
  private static HttpServer jwksServer;

  @Autowired private JwtDecoder jwtDecoder;

  @DynamicPropertySource
  static void registerJwksEndpoint(DynamicPropertyRegistry registry) {
    startJwksServer();
    registry.add(
        "app.service-auth.resource-server.jwk-set-uri",
        () -> "http://localhost:" + jwksServer.getAddress().getPort() + "/jwks");
  }

  @Test
  void decodesSignedJwtAndRejectsInvalidSignatureAndClaims() throws Exception {
    RSAKey unrelatedKey = newRsaKey("trusted-key");

    assertThat(jwtDecoder.decode(signedJwt(signingKey, ISSUER, List.of(AUDIENCE), true)))
        .isNotNull();
    assertThatThrownBy(
            () -> jwtDecoder.decode(signedJwt(unrelatedKey, ISSUER, List.of(AUDIENCE), true)))
        .isInstanceOf(BadJwtException.class);
    assertThatThrownBy(
            () ->
                jwtDecoder.decode(signedJwt(signingKey, ISSUER, List.of("another-service"), true)))
        .isInstanceOf(JwtValidationException.class);
    assertThatThrownBy(
            () -> jwtDecoder.decode(signedJwt(signingKey, ISSUER, List.of(AUDIENCE), false)))
        .isInstanceOf(JwtValidationException.class);
  }

  @Test
  void rejectsMissingHumanAndMalformedTokenTypes() throws Exception {
    for (Object tokenType : new Object[] {null, "human", List.of("service"), 123}) {
      Instant now = Instant.now();
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .issuer(ISSUER)
              .audience(AUDIENCE)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plusSeconds(300)))
              .claim("token_type", tokenType)
              .build();
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
              claims);
      jwt.sign(new RSASSASigner(signingKey));
      assertThatThrownBy(() -> jwtDecoder.decode(jwt.serialize()))
          .isInstanceOf(JwtValidationException.class)
          .hasMessageContaining("Token is not a service token");
    }
  }

  @AfterAll
  static void stopJwksServer() {
    if (jwksServer != null) {
      jwksServer.stop(0);
    }
  }

  private static void startJwksServer() {
    try {
      signingKey = newRsaKey("trusted-key");
      jwksServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      byte[] jwks =
          new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
      jwksServer.createContext(
          "/jwks",
          exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var responseBody = exchange.getResponseBody()) {
              responseBody.write(jwks);
            }
          });
      jwksServer.start();
    } catch (IOException | JOSEException exception) {
      throw new IllegalStateException("Could not start JWKS test endpoint", exception);
    }
  }

  private static RSAKey newRsaKey(String keyId) throws JOSEException {
    return new RSAKeyGenerator(2048).keyID(keyId).generate();
  }

  private static String signedJwt(
      RSAKey key, String issuer, List<String> audience, boolean includeExpiration)
      throws JOSEException {
    Instant now = Instant.now();
    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .issueTime(Date.from(now))
            .claim("token_type", "service");
    if (includeExpiration) {
      claims.expirationTime(Date.from(now.plusSeconds(60)));
    }

    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
            claims.build());
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }
}
