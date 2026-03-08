package com.tychewealth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI userServiceOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Tyche-Wealth - User Service API")
                .version("v1")
                .description(
                    "OpenAPI documentation for the User Service of Tyche-Wealth. "
                        + "This service is responsible for managing user accounts, authentication "
                        + "and user-related portfolio information.")
                .contact(
                    new Contact().name("Ismael Perez").url("https://github.com/ismaelperezdev")));
  }
}
