package com.tychewealth.config.properties;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
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
    Duration connectTimeout,
    Duration requestTimeout,
    int maxRetries,
    Duration retryBackoff,
    @NotBlank String defaultInterval) {}
