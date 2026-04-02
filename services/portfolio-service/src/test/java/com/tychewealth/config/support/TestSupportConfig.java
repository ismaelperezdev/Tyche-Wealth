package com.tychewealth.config.support;

import static com.tychewealth.constants.TestConstants.TEST_JWT_SECRET;

import com.tychewealth.config.IntegrationTestConfig;
import com.tychewealth.config.JwtAuthenticationFilter;
import com.tychewealth.config.PortfolioRateLimitConfig;
import com.tychewealth.config.security.ApplicationSecurityConfig;
import com.tychewealth.config.security.PrometheusSecurityConfig;
import com.tychewealth.config.security.SecurityCommonConfig;
import com.tychewealth.controller.impl.PortfolioApiController;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.asset.AssetMapperImpl;
import com.tychewealth.mapper.portfolio.PortfolioMapperImpl;
import com.tychewealth.ratelimit.RateLimitStore;
import com.tychewealth.ratelimit.support.PortfolioRateLimitSupport;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import com.tychewealth.service.impl.PortfolioServiceImpl;
import com.tychewealth.service.token.TokenValidator;
import com.tychewealth.service.token.support.AccessTokenSupport;
import com.tychewealth.service.token.support.TokenStateSupport;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import com.tychewealth.testhelper.TestRedisSupport;
import java.time.Clock;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
@Import({
  IntegrationTestConfig.class,
  PortfolioApiController.class,
  PortfolioServiceImpl.class,
  PortfolioValidationHelper.class,
  ErrorHandler.class,
  PortfolioMapperImpl.class,
  AssetMapperImpl.class,
  JwtAuthenticationFilter.class,
  PortfolioRateLimitConfig.class,
  SecurityCommonConfig.class,
  ApplicationSecurityConfig.class,
  PrometheusSecurityConfig.class,
  PortfolioRateLimitSupport.class,
  AccessTokenSupport.class,
  TokenStateSupport.class,
  TokenValidator.class
})
@EnableJpaRepositories(basePackageClasses = {PortfolioRepository.class, AssetRepository.class})
@EntityScan(basePackageClasses = {PortfolioEntity.class, AssetEntity.class})
public class TestSupportConfig {

  @Bean
  public TestRedisSupport.InMemoryRedisState testRedisState() {
    return new TestRedisSupport.InMemoryRedisState();
  }

  @Bean
  public RedisTemplate<String, String> redisTemplate(
      TestRedisSupport.InMemoryRedisState testRedisState) {
    return TestRedisSupport.redisTemplate(testRedisState);
  }

  @Bean
  public RateLimitStore rateLimitStore() {
    return new InMemoryRateLimitStore(Clock.systemUTC());
  }

  public static void applyCommonProperties(ConfigurableApplicationContext applicationContext) {
    TestPropertyValues.of(
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.liquibase.enabled=true",
            "spring.liquibase.change-log=classpath:db.changelog/changelog-master.xml",
            "spring.data.redis.repositories.enabled=false",
            "app.auth.jwt.secret=" + TEST_JWT_SECRET,
            "app.portfolio.create-rate-limit.max-requests=50",
            "app.portfolio.create-rate-limit.window-seconds=60")
        .applyTo(applicationContext.getEnvironment());
  }
}
