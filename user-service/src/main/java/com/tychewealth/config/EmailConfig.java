package com.tychewealth.config;

import com.tychewealth.dto.email.ResendEmailPropertiesDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.service.ratelimit.RateLimitStore;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
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
  public EmailSender emailSender(
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
    Assert.isTrue(emailDailyLimit > 0, "app.email.daily-limit must be a positive integer");

    RestClient restClient = restClientBuilder.baseUrl(resendEmailProperties.getBaseUrl()).build();
    return new EmailSender(
        restClient, resendEmailProperties, emailDailyLimit, rateLimitStore, emailClock);
  }

  @Bean
  public URI verifyRegistrationUri(
      @Value("${app.auth.verify-registration-url}") String verifyRegistrationUrl) {
    Assert.hasText(verifyRegistrationUrl, "app.auth.verify-registration-url must be configured");
    return parseAbsoluteUri(verifyRegistrationUrl, "app.auth.verify-registration-url");
  }

  @Bean
  public URI verifyLoginDeviceUri(
      @Value("${app.auth.verify-login-device-url}") String verifyLoginDeviceUrl) {
    Assert.hasText(verifyLoginDeviceUrl, "app.auth.verify-login-device-url must be configured");
    return parseAbsoluteUri(verifyLoginDeviceUrl, "app.auth.verify-login-device-url");
  }

  @Bean
  public URI forgotPasswordUri(@Value("${app.auth.forgot-password-url}") String forgotPasswordUrl) {
    Assert.hasText(forgotPasswordUrl, "app.auth.forgot-password-url must be configured");
    return parseAbsoluteUri(forgotPasswordUrl, "app.auth.forgot-password-url");
  }

  private URI parseAbsoluteUri(String value, String propertyName) {
    try {
      URI uri = new URI(value);
      Assert.isTrue(uri.isAbsolute(), propertyName + " must be an absolute URL");
      Assert.hasText(uri.getScheme(), propertyName + " must include a scheme");
      Assert.hasText(uri.getHost(), propertyName + " must include a host");
      return uri;
    } catch (URISyntaxException ex) {
      throw new IllegalStateException(propertyName + " must be a valid URL", ex);
    }
  }
}
