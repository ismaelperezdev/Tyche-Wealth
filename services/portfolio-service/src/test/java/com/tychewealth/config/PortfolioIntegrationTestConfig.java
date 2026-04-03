package com.tychewealth.config;

import com.tychewealth.config.support.TestSupportConfig;
import com.tychewealth.controller.impl.PortfolioApiController;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import com.tychewealth.service.impl.PortfolioServiceImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({
  TestSupportConfig.class,
  PortfolioApiController.class,
  PortfolioServiceImpl.class,
  PortfolioValidationHelper.class
})
public class PortfolioIntegrationTestConfig {

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
      TestSupportConfig.applyCommonProperties(applicationContext);
    }
  }
}
