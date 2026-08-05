package com.tychewealth.dto.ratelimit;

import com.tychewealth.ratelimit.RateLimitInterceptor;

/** Associates an authentication route pattern with its rate-limit namespace and interceptor. */
public record AuthRateLimitRegistrationDto(
    String pathPattern, String namespace, RateLimitInterceptor interceptor) {}
