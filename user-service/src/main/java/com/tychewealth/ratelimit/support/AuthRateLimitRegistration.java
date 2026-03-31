package com.tychewealth.ratelimit.support;

import com.tychewealth.ratelimit.RateLimitInterceptor;
import lombok.Getter;

@Getter
public class AuthRateLimitRegistration {

  private final String pathPattern;
  private final RateLimitInterceptor interceptor;

  public AuthRateLimitRegistration(String pathPattern, RateLimitInterceptor interceptor) {
    this.pathPattern = pathPattern;
    this.interceptor = interceptor;
  }
}
