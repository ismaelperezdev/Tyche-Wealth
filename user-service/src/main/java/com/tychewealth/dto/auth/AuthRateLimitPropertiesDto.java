package com.tychewealth.dto.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthRateLimitPropertiesDto(
    @Valid RateLimit loginRateLimit,
    @Valid RateLimit registerRateLimit,
    @Valid RateLimit refreshRateLimit,
    @Valid RateLimit forgotPasswordRateLimit,
    @Valid RateLimit resendVerificationRateLimit,
    @Valid RateLimit verifyLoginDeviceRateLimit) {

  public AuthRateLimitPropertiesDto {
    loginRateLimit = loginRateLimit == null ? new RateLimit(10, 60) : loginRateLimit;
    registerRateLimit = registerRateLimit == null ? new RateLimit(5, 300) : registerRateLimit;
    refreshRateLimit = refreshRateLimit == null ? new RateLimit(10, 60) : refreshRateLimit;
    forgotPasswordRateLimit =
        forgotPasswordRateLimit == null ? new RateLimit(2, 3600) : forgotPasswordRateLimit;
    resendVerificationRateLimit =
        resendVerificationRateLimit == null ? new RateLimit(2, 3600) : resendVerificationRateLimit;
    verifyLoginDeviceRateLimit =
        verifyLoginDeviceRateLimit == null ? new RateLimit(3, 900) : verifyLoginDeviceRateLimit;
  }

  public record RateLimit(@Positive int maxRequests, @Positive long windowSeconds) {}
}
