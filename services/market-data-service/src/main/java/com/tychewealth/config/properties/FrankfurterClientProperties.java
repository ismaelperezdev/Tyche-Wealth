package com.tychewealth.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clients.frankfurter")
public record FrankfurterClientProperties(
    @NotBlank String baseUrl, @NotNull @DurationMin(millis = 1) Duration requestTimeout) {}
