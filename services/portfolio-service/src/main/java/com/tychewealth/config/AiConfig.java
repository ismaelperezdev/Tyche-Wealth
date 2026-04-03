package com.tychewealth.config;

import com.tychewealth.dto.ai.AiPropertiesDto;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiPropertiesDto.class)
public class AiConfig {

  @Bean
  public HttpClient aiHttpClient(AiPropertiesDto aiProperties) {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(aiProperties.connectTimeoutSeconds()))
        .build();
  }
}
