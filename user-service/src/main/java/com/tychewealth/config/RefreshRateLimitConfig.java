package com.tychewealth.config;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static com.tychewealth.constants.AuthConstants.LOGIN_RATE_LIMIT_MESSAGE;
import static com.tychewealth.constants.AuthConstants.REGISTER_RATE_LIMIT_MESSAGE;

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

  /**
   * Constructs a RefreshRateLimitConfig and initializes per-endpoint rate-limit interceptors for
   * login, register, and refresh flows.
   *
   * Reads rate-limit settings from {@code properties} and creates:
   * - a login interceptor with configured limits and metrics hooks,
   * - a register interceptor with configured limits and metrics hooks,
   * - a refresh interceptor with configured limits and metrics hooks.
   *
   * @param properties  provides configured rate-limit values for login, register, and refresh endpoints
   * @param authMetrics records request and rate-limited events for the created interceptors
   */
  public RefreshRateLimitConfig(AuthRateLimitProperties properties, AuthMetrics authMetrics) {
    AuthRateLimitProperties.RateLimit login = properties.getLoginRateLimit();
    AuthRateLimitProperties.RateLimit register = properties.getRegisterRateLimit();
    AuthRateLimitProperties.RateLimit refresh = properties.getRefreshRateLimit();

    this.loginRateLimitInterceptor =
        new AuthRateLimitInterceptor(
            login.getMaxRequests(),
            login.getWindowSeconds(),
            LOGIN_RATE_LIMIT_MESSAGE,
            ignored -> authMetrics.recordLoginRequest(),
            ignored -> authMetrics.recordLoginRateLimited());
    this.registerRateLimitInterceptor =
        new AuthRateLimitInterceptor(
            register.getMaxRequests(),
            register.getWindowSeconds(),
            REGISTER_RATE_LIMIT_MESSAGE,
            ignored -> authMetrics.recordRegisterRequest(),
            ignored -> authMetrics.recordRegisterRateLimited());
    this.refreshRateLimitInterceptor =
        new RefreshRateLimitInterceptor(
            refresh.getMaxRequests(), refresh.getWindowSeconds(), authMetrics);
  }

  /**
   * Exposes the configured interceptor that enforces login rate limits as a Spring bean.
   *
   * @return the `AuthRateLimitInterceptor` instance used to enforce rate limits on login requests
   */
  @Bean
  public AuthRateLimitInterceptor loginRateLimitInterceptor() {
    return loginRateLimitInterceptor;
  }

  /**
   * Exposes the configured registration rate-limit interceptor as a Spring bean.
   *
   * @return the configured {@link AuthRateLimitInterceptor} used to enforce registration rate limits
   */
  @Bean
  public AuthRateLimitInterceptor registerRateLimitInterceptor() {
    return registerRateLimitInterceptor;
  }

  /**
   * Exposes the configured RefreshRateLimitInterceptor as a Spring bean.
   *
   * @return the configured RefreshRateLimitInterceptor used to enforce rate limits on refresh requests
   */
  @Bean
  public RefreshRateLimitInterceptor refreshRateLimitInterceptor() {
    return refreshRateLimitInterceptor;
  }

  /**
   * Registers rate-limiting interceptors for authentication endpoints.
   *
   * <p>Associates the register, login, and refresh interceptors with AUTH_REGISTER_URL,
   * AUTH_LOGIN_URL, and AUTH_REFRESH_URL respectively.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(registerRateLimitInterceptor).addPathPatterns(AUTH_REGISTER_URL);
    registry.addInterceptor(loginRateLimitInterceptor).addPathPatterns(AUTH_LOGIN_URL);
    registry.addInterceptor(refreshRateLimitInterceptor).addPathPatterns(AUTH_REFRESH_URL);
  }

  /**
   * Reset the state of all authentication rate-limit interceptors.
   *
   * Invokes `reset()` on the register, login, and refresh interceptors to clear their internal rate-limiting state (counters and windows).
   */
  public void resetAll() {
    registerRateLimitInterceptor.reset();
    loginRateLimitInterceptor.reset();
    refreshRateLimitInterceptor.reset();
  }
}
