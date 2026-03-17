package com.tychewealth.config;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;

import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.monitoring.AuthMetrics;
import com.tychewealth.service.ratelimit.RateLimitStore;
import com.tychewealth.web.AuthRateLimitInterceptor;
import com.tychewealth.web.RefreshRateLimitInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AuthRateLimitProperties.class)
public class RefreshRateLimitConfig implements WebMvcConfigurer {
  private static final String LOGIN_NAMESPACE = "rate-limit:auth:login";
  private static final String REGISTER_NAMESPACE = "rate-limit:auth:register";
  private static final String REFRESH_NAMESPACE = "rate-limit:auth:refresh";

  private final RefreshRateLimitInterceptor refreshRateLimitInterceptor;
  private final AuthRateLimitInterceptor loginRateLimitInterceptor;
  private final AuthRateLimitInterceptor registerRateLimitInterceptor;

  public RefreshRateLimitConfig(
      AuthRateLimitProperties properties, AuthMetrics authMetrics, RateLimitStore rateLimitStore) {
    AuthRateLimitProperties.RateLimit login = properties.getLoginRateLimit();
    AuthRateLimitProperties.RateLimit register = properties.getRegisterRateLimit();
    AuthRateLimitProperties.RateLimit refresh = properties.getRefreshRateLimit();

    this.loginRateLimitInterceptor =
        new AuthRateLimitInterceptor(
            LOGIN_NAMESPACE,
            login.getMaxRequests(),
            login.getWindowSeconds(),
            ErrorDefinition.RATE_LIMITED.getDescription(),
            ignored -> authMetrics.recordLoginRequest(),
            ignored -> authMetrics.recordLoginRateLimited(),
            rateLimitStore);
    this.registerRateLimitInterceptor =
        new AuthRateLimitInterceptor(
            REGISTER_NAMESPACE,
            register.getMaxRequests(),
            register.getWindowSeconds(),
            ErrorDefinition.RATE_LIMITED.getDescription(),
            ignored -> authMetrics.recordRegisterRequest(),
            ignored -> authMetrics.recordRegisterRateLimited(),
            rateLimitStore);
    this.refreshRateLimitInterceptor =
        new RefreshRateLimitInterceptor(
            REFRESH_NAMESPACE,
            refresh.getMaxRequests(),
            refresh.getWindowSeconds(),
            authMetrics,
            rateLimitStore);
  }

  @Bean
  public AuthRateLimitInterceptor loginRateLimitInterceptor() {
    return loginRateLimitInterceptor;
  }

  @Bean
  public AuthRateLimitInterceptor registerRateLimitInterceptor() {
    return registerRateLimitInterceptor;
  }

  @Bean
  public RefreshRateLimitInterceptor refreshRateLimitInterceptor() {
    return refreshRateLimitInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(registerRateLimitInterceptor).addPathPatterns(AUTH_REGISTER_URL);
    registry.addInterceptor(loginRateLimitInterceptor).addPathPatterns(AUTH_LOGIN_URL);
    registry.addInterceptor(refreshRateLimitInterceptor).addPathPatterns(AUTH_REFRESH_URL);
  }

  public void resetAll() {
    registerRateLimitInterceptor.reset();
    loginRateLimitInterceptor.reset();
    refreshRateLimitInterceptor.reset();
  }
}
