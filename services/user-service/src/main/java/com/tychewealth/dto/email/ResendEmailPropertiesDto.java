package com.tychewealth.dto.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.email.resend")
public class ResendEmailPropertiesDto {

  private boolean enabled = false;
  private String apiKey;
  private String from;
  private String baseUrl = "https://api.resend.com";
}
