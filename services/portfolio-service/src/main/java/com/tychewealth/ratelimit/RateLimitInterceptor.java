package com.tychewealth.ratelimit;

import static com.tychewealth.constants.LogConstants.AUTH;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_ACTION;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT;
import static com.tychewealth.constants.LogConstants.RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;

import com.tychewealth.dto.ratelimit.RateLimitPropertiesDto;
import com.tychewealth.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

  private final RateLimitPropertiesDto properties;
  private final RateLimitStore rateLimitStore;

  public RateLimitInterceptor(RateLimitPropertiesDto properties, RateLimitStore rateLimitStore) {
    if (properties == null) {
      throw new IllegalArgumentException("properties must not be null");
    }
    if (rateLimitStore == null) {
      throw new IllegalArgumentException("rateLimitStore must not be null");
    }
    this.properties = properties;
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    RateLimited rateLimited = findRateLimited(handlerMethod);
    if (rateLimited == null) {
      return true;
    }

    RateLimitKey key = rateLimited.value();
    RateLimitPropertiesDto.RateLimitDto rule = properties.ruleFor(key);

    long requestCount;
    try {
      requestCount =
          rateLimitStore.increment(
              key.namespace(),
              resolveClientKey(request),
              Duration.ofSeconds(rule.getWindowSeconds()));
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT,
          AUTH,
          RATE_LIMIT_ACTION,
          RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE,
          request.getRequestURI(),
          key.namespace(),
          null,
          ex);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Rate limit service unavailable");
    }

    if (requestCount > rule.getMaxRequests()) {
      throw Utils.rateLimited(null);
    }

    return true;
  }

  private RateLimited findRateLimited(HandlerMethod handlerMethod) {
    RateLimited rateLimited =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RateLimited.class);
    if (rateLimited != null) {
      return rateLimited;
    }
    return AnnotatedElementUtils.findMergedAnnotation(
        handlerMethod.getBeanType(), RateLimited.class);
  }

  private String resolveClientKey(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Long userId) {
      return "user:" + userId;
    }

    String remoteAddress = request.getRemoteAddr();
    return remoteAddress == null || remoteAddress.isBlank() ? "anonymous" : "ip:" + remoteAddress;
  }
}
