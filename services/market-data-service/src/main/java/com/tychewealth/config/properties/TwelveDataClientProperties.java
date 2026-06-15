package com.tychewealth.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clients.twelve-data")
public record TwelveDataClientProperties(
    @NotBlank String baseUrl,
    @NotBlank String pricePath,
    @NotBlank String quotePath,
    @NotBlank String timeSeriesPath,
    @NotBlank String apiKey,
    @NotNull @DurationMin(millis = 1) Duration connectTimeout,
    @NotNull @DurationMin(millis = 1) Duration requestTimeout,
    @Positive int maxRetries,
    @NotNull @DurationMin(millis = 1) Duration retryBackoff,
    @NotBlank String defaultInterval) {}
