package com.tychewealth.config;

import com.tychewealth.client.TwelveDataApiClient;
import com.tychewealth.config.properties.TwelveDataClientProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableConfigurationProperties(TwelveDataClientProperties.class)
@Import({WebClientConfig.class, TwelveDataApiClient.class})
public class ClientIntegrationTestConfig {}
