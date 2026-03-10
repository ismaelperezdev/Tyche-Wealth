package com.tychewealth.config;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;

import com.tychewealth.service.monitoring.AuthMetrics;
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

  private final RefreshRateLimitInterceptor refreshRateLimitInterceptor;
  private final AuthRateLimitInterceptor loginRateLimitInterceptor;
  private final AuthRateLimitInterceptor registerRateLimitInterceptor;

  public RefreshRateLimitConfig(AuthRateLimitProperties properties, AuthMetrics authMetrics) {
    AuthRateLimitProperties.RateLimit login = properties.getLoginRateLimit();
    AuthRateLimitProperties.RateLimit register = properties.getRegisterRateLimit();
    AuthRateLimitProperties.RateLimit refresh = properties.getRefreshRateLimit();

    this.loginRateLimitInterceptor =
        new AuthRateLimitInterceptor(
            login.getMaxRequests(),
            login.getWindowSeconds(),
            "Too many login requests",
            ignored -> authMetrics.recordLoginRequest(),
            ignored -> authMetrics.recordLoginRateLimited());
    this.registerRateLimitInterceptor =
        new AuthRateLimitInterceptor(
            register.getMaxRequests(),
            register.getWindowSeconds(),
            "Too many register requests",
            ignored -> authMetrics.recordRegisterRequest(),
            ignored -> authMetrics.recordRegisterRateLimited());
    this.refreshRateLimitInterceptor =
        new RefreshRateLimitInterceptor(
            refresh.getMaxRequests(), refresh.getWindowSeconds(), authMetrics);
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
