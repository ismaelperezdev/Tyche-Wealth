package com.tychewealth.config;

import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;

import com.tychewealth.dto.auth.AuthRateLimitPropertiesDto;
import com.tychewealth.monitoring.AuthMetrics;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.support.AuthRateLimitRegistration;
import com.tychewealth.ratelimit.support.AuthRateLimitSupport;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AuthRateLimitPropertiesDto.class)
public class AuthRateLimitConfig implements WebMvcConfigurer {
  private final List<AuthRateLimitRegistration> authRateLimitRegistrations;
  private final RateLimitInterceptor refreshRateLimitInterceptor;

  public AuthRateLimitConfig(
      AuthRateLimitPropertiesDto properties,
      AuthMetrics authMetrics,
      AuthRateLimitSupport authRateLimitSupport) {
    this.authRateLimitRegistrations =
        authRateLimitSupport.buildAuthRegistrations(properties, authMetrics);
    this.refreshRateLimitInterceptor =
        authRateLimitSupport.buildRefreshInterceptor(properties.refreshRateLimit(), authMetrics);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    for (AuthRateLimitRegistration registration : authRateLimitRegistrations) {
      registry
          .addInterceptor(registration.getInterceptor())
          .addPathPatterns(registration.getPathPattern());
    }
    registry.addInterceptor(refreshRateLimitInterceptor).addPathPatterns(AUTH_REFRESH_URL);
  }

  public void resetAll() {
    for (AuthRateLimitRegistration registration : authRateLimitRegistrations) {
      registration.getInterceptor().reset();
    }
    refreshRateLimitInterceptor.reset();
  }
}
