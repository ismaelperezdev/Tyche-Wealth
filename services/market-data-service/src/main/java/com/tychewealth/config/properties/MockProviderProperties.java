package com.tychewealth.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.provider")
public record MockProviderProperties(
    boolean enabled,
    int port,
    int minVehiclesPerResponse,
    int maxVehiclesPerResponse,
    int minVehiclesChangedPerRefresh,
    int maxVehiclesChangedPerRefresh,
    int minResponseDurationSeconds,
    int maxResponseDurationSeconds) {}
