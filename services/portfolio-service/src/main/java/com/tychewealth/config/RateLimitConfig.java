package com.tychewealth.config;

import com.tychewealth.dto.ratelimit.RateLimitPropertiesDto;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimitStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitPropertiesDto.class)
public class RateLimitConfig implements WebMvcConfigurer {

  private final RateLimitInterceptor rateLimitInterceptor;
  private final RateLimitStore rateLimitStore;

  public RateLimitConfig(RateLimitPropertiesDto properties, RateLimitStore rateLimitStore) {
    this.rateLimitInterceptor = new RateLimitInterceptor(properties, rateLimitStore);
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
  }

  public void resetAll() {
    for (RateLimitKey key : RateLimitKey.values()) {
      rateLimitStore.resetNamespace(key.namespace());
    }
  }
}
