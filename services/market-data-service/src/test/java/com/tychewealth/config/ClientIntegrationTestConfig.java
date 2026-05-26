package com.tychewealth.config;

import com.tychewealth.client.VehicleApiClient;
import com.tychewealth.config.properties.ResourcesClientProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ResourcesClientProperties.class)
@Import({WebClientConfig.class, VehicleApiClient.class})
public class ClientIntegrationTestConfig {}
