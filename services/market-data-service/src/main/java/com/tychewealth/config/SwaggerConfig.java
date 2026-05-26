package com.tychewealth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String API_TITLE = "Tyche Wealth - Market Data Service";
  private static final String API_VERSION = "v1";
  private static final String API_DESCRIPTION =
      """
            This service provides market data for Tyche Wealth.

            It periodically retrieves market information from external providers, normalizes the payloads, and refreshes cached snapshots used by portfolio and chart features.

            The API exposes market-oriented endpoints designed for fast reads and stable client integration.
            """;
  private static final String AUTHOR_NAME = "Tyche Wealth";

  @Bean
  OpenAPI marketDataOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title(API_TITLE)
                .version(API_VERSION)
                .description(API_DESCRIPTION)
                .contact(new Contact().name(AUTHOR_NAME)));
  }
}
