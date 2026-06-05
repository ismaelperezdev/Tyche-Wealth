package com.tychewealth.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients.twelve-data")
public record TwelveDataClientProperties(
    String baseUrl,
    String pricePath,
    String quotePath,
    String timeSeriesPath,
    String apiKey,
    Duration connectTimeout,
    Duration requestTimeout,
    int maxRetries,
    Duration retryBackoff,
    String defaultInterval) {}
