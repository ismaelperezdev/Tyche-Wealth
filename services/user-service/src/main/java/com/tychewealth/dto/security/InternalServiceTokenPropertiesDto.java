package com.tychewealth.dto.security;

import static com.tychewealth.constants.AuthConstants.BCRYPT_MAX_PASSWORD_BYTES;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configures the resource audiences allowed in internal service tokens. */
@Validated
@ConfigurationProperties(prefix = "app.oauth2.authorization-server")
public record InternalServiceTokenPropertiesDto(
    @NotEmpty Set<@NotBlank String> allowedAudiences,
    @NotEmpty List<@Valid ServiceClientProperties> clients) {

  public record ServiceClientProperties(
      @NotBlank String clientId,
      @NotBlank String clientSecret,
      @NotEmpty Set<@NotBlank String> scopes,
      @NotEmpty Set<@NotBlank String> audiences,
      @Positive long accessTokenTtlSeconds) {

    @AssertTrue(message = MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED)
    public boolean isClientSecretWithinBcryptLimit() {
      return clientSecret == null
          || clientSecret.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_PASSWORD_BYTES;
    }
  }
}
