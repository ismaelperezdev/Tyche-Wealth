package com.tychewealth.config;

import com.tychewealth.dto.email.ResendEmailPropertiesDto;
import com.tychewealth.service.helper.email.EmailServiceHelper;
import com.tychewealth.service.ratelimit.RateLimitStore;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ResendEmailPropertiesDto.class)
public class EmailConfig {

  @Bean
  public Clock emailClock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.email.resend", name = "enabled", havingValue = "true")
  public EmailServiceHelper emailServiceHelper(
      RestClient.Builder restClientBuilder,
      ResendEmailPropertiesDto resendEmailProperties,
      @Value("${app.email.daily-limit:80}") int emailDailyLimit,
      RateLimitStore rateLimitStore,
      Clock emailClock) {
    Assert.hasText(
        resendEmailProperties.getApiKey(),
        "app.email.resend.api-key must be configured when Resend is enabled");
    Assert.hasText(
        resendEmailProperties.getFrom(),
        "app.email.resend.from must be configured when Resend is enabled");

    RestClient restClient = restClientBuilder.baseUrl(resendEmailProperties.getBaseUrl()).build();
    return new EmailServiceHelper(
        restClient, resendEmailProperties, emailDailyLimit, rateLimitStore, emailClock);
  }
}
