package com.tychewealth.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configures the resource audiences allowed in internal service tokens. */
@Validated
@ConfigurationProperties(prefix = "app.oauth2.authorization-server")
public record InternalServiceTokenPropertiesDto(
    @NotEmpty Set<@NotBlank String> allowedAudiences,
    @NotEmpty Set<@NotBlank String> allowedClientIds) {}
