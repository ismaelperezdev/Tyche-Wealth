package com.tychewealth.dto.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties required to connect the email sender to Resend.
 *
 * <p>Binds the provider credentials and base URL from the {@code app.email.resend} configuration
 * namespace. The default URL targets the public Resend API, while credentials are supplied by the
 * deployment environment.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.email.resend")
public class ResendEmailPropertiesDto {

  private boolean enabled = false;
  private String apiKey;
  private String from;
  private String baseUrl = "https://api.resend.com";
}
