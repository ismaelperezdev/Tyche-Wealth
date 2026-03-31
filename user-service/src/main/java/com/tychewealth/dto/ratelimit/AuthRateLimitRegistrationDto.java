package com.tychewealth.dto.ratelimit;

import com.tychewealth.ratelimit.RateLimitInterceptor;

public record AuthRateLimitRegistrationDto(
    String pathPattern, String namespace, RateLimitInterceptor interceptor) {}
