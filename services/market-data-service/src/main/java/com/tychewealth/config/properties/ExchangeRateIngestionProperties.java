package com.tychewealth.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.exchange-rate-ingestion")
public record ExchangeRateIngestionProperties(
    @NotBlank String baseCurrency, @NotEmpty List<@NotBlank String> quoteCurrencies) {}
