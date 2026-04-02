package com.tychewealth.ratelimit.support;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;

import com.tychewealth.dto.ratelimit.PortfolioRateLimitPropertiesDto;
import com.tychewealth.dto.ratelimit.RateLimitCallbacksDto;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.RateLimitStore;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PortfolioRateLimitSupport {

  private static final String CREATE_PORTFOLIO_NAMESPACE = "rate-limit:portfolio:create";

  private final RateLimitStore rateLimitStore;

  public PortfolioRateLimitSupport(RateLimitStore rateLimitStore) {
    this.rateLimitStore = rateLimitStore;
  }

  public RateLimitInterceptor buildCreatePortfolioInterceptor(
      PortfolioRateLimitPropertiesDto.RateLimitDto rateLimit) {
    Duration window = Duration.ofSeconds(rateLimit.windowSeconds());
    return new RateLimitInterceptor(
        new RateLimitInterceptorConfig(
            CREATE_PORTFOLIO_NAMESPACE,
            rateLimit.maxRequests(),
            window,
            ErrorDefinition.RATE_LIMITED.getDescription(),
            RateLimitCallbacksDto.none(),
            true,
            this::resolveClientKey),
        rateLimitStore);
  }

  public String createPortfolioPathPattern() {
    return PORTFOLIO_BASE_URL;
  }

  public String createPortfolioNamespace() {
    return CREATE_PORTFOLIO_NAMESPACE;
  }

  private String resolveClientKey(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Long userId) {
      return "user:" + userId;
    }

    String remoteAddress = request == null ? null : request.getRemoteAddr();
    return remoteAddress == null || remoteAddress.isBlank() ? "anonymous" : "ip:" + remoteAddress;
  }
}
