package com.tychewealth.config;

import com.tychewealth.dto.ratelimit.PortfolioRateLimitPropertiesDto;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitStore;
import com.tychewealth.ratelimit.support.PortfolioRateLimitSupport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(PortfolioRateLimitPropertiesDto.class)
public class PortfolioRateLimitConfig implements WebMvcConfigurer {

  private final RateLimitInterceptor createPortfolioRateLimitInterceptor;
  private final RateLimitStore rateLimitStore;
  private final String createPortfolioNamespace;
  private final String createPortfolioPathPattern;

  public PortfolioRateLimitConfig(
      PortfolioRateLimitPropertiesDto properties,
      PortfolioRateLimitSupport portfolioRateLimitSupport,
      RateLimitStore rateLimitStore) {
    this.createPortfolioRateLimitInterceptor =
        portfolioRateLimitSupport.buildCreatePortfolioInterceptor(properties.createRateLimit());
    this.rateLimitStore = rateLimitStore;
    this.createPortfolioNamespace = portfolioRateLimitSupport.createPortfolioNamespace();
    this.createPortfolioPathPattern = portfolioRateLimitSupport.createPortfolioPathPattern();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(createPortfolioRateLimitInterceptor)
        .addPathPatterns(createPortfolioPathPattern);
  }

  public void resetAll() {
    rateLimitStore.resetNamespace(createPortfolioNamespace);
  }
}
