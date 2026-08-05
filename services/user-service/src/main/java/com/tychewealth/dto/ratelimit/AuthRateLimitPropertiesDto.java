package com.tychewealth.dto.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds and validates the request quotas configured for authentication endpoints.
 *
 * <p>Provides one policy per protected authentication flow and supplies conservative defaults when
 * a policy is omitted. Each policy defines the maximum number of requests and the duration of its
 * rate-limit window in seconds.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthRateLimitPropertiesDto(
    @Valid RateLimitDto loginRateLimit,
    @Valid RateLimitDto registerRateLimit,
    @Valid RateLimitDto refreshRateLimit,
    @Valid RateLimitDto forgotPasswordRateLimit,
    @Valid RateLimitDto resendVerificationRateLimit,
    @Valid RateLimitDto verifyLoginDeviceRateLimit) {

  public AuthRateLimitPropertiesDto {
    loginRateLimit = loginRateLimit == null ? new RateLimitDto(10, 60) : loginRateLimit;
    registerRateLimit = registerRateLimit == null ? new RateLimitDto(5, 300) : registerRateLimit;
    refreshRateLimit = refreshRateLimit == null ? new RateLimitDto(10, 60) : refreshRateLimit;
    forgotPasswordRateLimit =
        forgotPasswordRateLimit == null ? new RateLimitDto(2, 3600) : forgotPasswordRateLimit;
    resendVerificationRateLimit =
        resendVerificationRateLimit == null
            ? new RateLimitDto(2, 3600)
            : resendVerificationRateLimit;
    verifyLoginDeviceRateLimit =
        verifyLoginDeviceRateLimit == null ? new RateLimitDto(3, 900) : verifyLoginDeviceRateLimit;
  }

  /** Describes the request quota and time window for one rate-limited operation. */
  public record RateLimitDto(@Positive int maxRequests, @Positive long windowSeconds) {}
}
