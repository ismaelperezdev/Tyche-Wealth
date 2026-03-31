package com.tychewealth.config;

import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;

import com.tychewealth.dto.ratelimit.AuthRateLimitPropertiesDto;
import com.tychewealth.dto.ratelimit.AuthRateLimitRegistrationDto;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitStore;
import com.tychewealth.ratelimit.support.AuthRateLimitSupport;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AuthRateLimitPropertiesDto.class)
public class AuthRateLimitConfig implements WebMvcConfigurer {
  private final List<AuthRateLimitRegistrationDto> authRateLimitRegistrations;
  private final RateLimitInterceptor refreshRateLimitInterceptor;
  private final RateLimitStore rateLimitStore;
  private final String refreshNamespace;

  public AuthRateLimitConfig(
      AuthRateLimitPropertiesDto properties,
      AuthMetrics authMetrics,
      AuthRateLimitSupport authRateLimitSupport,
      RateLimitStore rateLimitStore) {
    this.authRateLimitRegistrations =
        authRateLimitSupport.buildAuthRegistrations(properties, authMetrics);
    this.refreshRateLimitInterceptor =
        authRateLimitSupport.buildRefreshInterceptor(properties.refreshRateLimit(), authMetrics);
    this.rateLimitStore = rateLimitStore;
    this.refreshNamespace = authRateLimitSupport.refreshNamespace();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    for (AuthRateLimitRegistrationDto registration : authRateLimitRegistrations) {
      registry
          .addInterceptor(registration.interceptor())
          .addPathPatterns(registration.pathPattern());
    }
    registry.addInterceptor(refreshRateLimitInterceptor).addPathPatterns(AUTH_REFRESH_URL);
  }

  public void resetAll() {
    for (AuthRateLimitRegistrationDto registration : authRateLimitRegistrations) {
      rateLimitStore.resetNamespace(registration.namespace());
    }
    rateLimitStore.resetNamespace(refreshNamespace);
  }
}
