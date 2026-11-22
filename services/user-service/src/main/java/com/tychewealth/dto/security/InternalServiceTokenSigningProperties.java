package com.tychewealth.dto.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuration for signing internal service access tokens. */
@Validated
@ConfigurationProperties(prefix = "app.oauth2.authorization-server.signing")
public record InternalServiceTokenSigningProperties(
    @NotBlank String issuer,
    @NotBlank String activeKeyId,
    @NotBlank String privateKeyLocation,
    List<@NotNull @Valid PreviousPublicKeyProperties> previousPublicKeys) {

  public InternalServiceTokenSigningProperties {
    previousPublicKeys = previousPublicKeys == null ? List.of() : List.copyOf(previousPublicKeys);
  }

  @AssertTrue(message = "signing key IDs must be unique")
  public boolean isKeyIdsUnique() {
    Set<String> keyIds = new HashSet<>();
    keyIds.add(activeKeyId);
    return previousPublicKeys.stream().allMatch(key -> keyIds.add(key.keyId()));
  }

  public record PreviousPublicKeyProperties(
      @NotBlank String keyId, @NotBlank String publicKeyLocation) {}
}
