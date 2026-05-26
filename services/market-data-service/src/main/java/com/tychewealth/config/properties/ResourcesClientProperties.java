package com.tychewealth.config.properties;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients.resources")
public record ResourcesClientProperties(
    String baseUrl,
    String resourcesPath,
    String lowerLeftLatLon,
    String upperRightLatLon,
    List<Integer> companyZoneIds,
    String userAgent,
    Duration requestTimeout,
    int maxRetries,
    Duration retryBackoff) {}
