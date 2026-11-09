package com.tychewealth.config;

import com.tychewealth.config.support.TestSupportConfig;
import com.tychewealth.controller.impl.PortfolioMarketDataApiController;
import com.tychewealth.mapper.marketdata.PortfolioMarketDataMapperImpl;
import com.tychewealth.service.helper.CommonValidationHelper;
import com.tychewealth.service.helper.marketdata.PortfolioMarketDataHelper;
import com.tychewealth.service.helper.marketdata.PortfolioMarketDataValidationHelper;
import com.tychewealth.service.impl.PortfolioMarketDataServiceImpl;
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
  PortfolioMarketDataApiController.class,
  PortfolioMarketDataServiceImpl.class,
  CommonValidationHelper.class,
  PortfolioMarketDataHelper.class,
  PortfolioMarketDataValidationHelper.class,
  PortfolioMarketDataMapperImpl.class
})
public class PortfolioMarketDataIntegrationTestConfig {

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
      TestSupportConfig.applyCommonProperties(applicationContext);
    }
  }
}
