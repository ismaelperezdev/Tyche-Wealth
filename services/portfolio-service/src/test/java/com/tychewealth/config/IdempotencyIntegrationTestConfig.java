package com.tychewealth.config;

import com.tychewealth.config.support.TestSupportConfig;
import com.tychewealth.controller.impl.AssetApiController;
import com.tychewealth.controller.impl.PortfolioApiController;
import com.tychewealth.service.helper.CommonValidationHelper;
import com.tychewealth.service.helper.asset.AssetCreateHelper;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ImportAssetsHelper;
import com.tychewealth.service.helper.asset.ai.AssetAiValidationHelper;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import com.tychewealth.service.impl.AssetServiceImpl;
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
  CommonValidationHelper.class,
  PortfolioValidationHelper.class,
  AssetApiController.class,
  AssetServiceImpl.class,
  AssetCreateHelper.class,
  AssetValidationHelper.class,
  AssetAiValidationHelper.class,
  ImportAssetsHelper.class
})
public class IdempotencyIntegrationTestConfig {

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
      TestSupportConfig.applyCommonProperties(applicationContext);
    }
  }
}
