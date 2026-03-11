package com.tychewealth.web;

import static com.tychewealth.constants.AuthConstants.REFRESH_RATE_LIMIT_MESSAGE;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tychewealth.service.monitoring.AuthMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshRateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final AuthMetrics authMetrics;
  private final Clock clock;
  private final Cache<String, Deque<Long>> requestsByClient;

  /**
   * Creates a RefreshRateLimitInterceptor that enforces per-client refresh request rate limits.
   *
   * @param maxRequests  the maximum number of allowed refresh requests per sliding window
   * @param windowSeconds  the length of the sliding window in seconds
   * @param authMetrics  collector for authentication-related metrics to record request and rate-limit events
   */
  public RefreshRateLimitInterceptor(int maxRequests, long windowSeconds, AuthMetrics authMetrics) {
    this(
        maxRequests,
        windowSeconds,
        authMetrics,
        buildRequestsByClientCache(windowSeconds),
        Clock.systemUTC());
  }

  /**
   * Create a RefreshRateLimitInterceptor that enforces per-client refresh request limits.
   *
   * @param maxRequests       maximum allowed requests per sliding window; must be greater than zero
   * @param windowSeconds     length of the sliding window in seconds; must be greater than zero
   * @param authMetrics       metrics collector for authentication-related events
   * @param requestsByClient  cache mapping client keys to deques of request timestamps
   * @param clock             time source used to timestamp requests
   * @throws IllegalArgumentException if {@code maxRequests} &le; 0 or {@code windowSeconds} &le; 0
   */
  RefreshRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      AuthMetrics authMetrics,
      Cache<String, Deque<Long>> requestsByClient,
      Clock clock) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Refresh rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Refresh rate limit window must be positive");
    }
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.authMetrics = authMetrics;
    this.requestsByClient = requestsByClient;
    this.clock = clock;
  }

  /**
   * Enforces per-client refresh request rate limiting before the request is handled.
   *
   * @param request the HTTP request used to resolve the client key
   * @param response the HTTP response (unused by this interceptor)
   * @param handler the chosen handler to execute (unused by this interceptor)
   * @return true to allow the request processing to continue
   * @throws org.springframework.web.server.ResponseStatusException if the client has exceeded
   *         the configured number of refresh requests within the time window (429 Too Many Requests)
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    authMetrics.recordRefreshRequest();

    String clientKey = resolveClientKey(request);
    long now = clock.millis();
    Deque<Long> timestamps = requestsByClient.get(clientKey, ignored -> new ArrayDeque<>());

    synchronized (timestamps) {
      evictExpiredRequests(timestamps, now);
      if (timestamps.size() >= maxRequests) {
        authMetrics.recordRefreshRateLimited();
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, REFRESH_RATE_LIMIT_MESSAGE);
      }
      timestamps.addLast(now);
    }

    return true;
  }

  /**
   * Removes client request timestamps that fall outside the configured sliding window.
   *
   * @param timestamps deque of request timestamps in milliseconds ordered from oldest to newest
   * @param now       current time in milliseconds used to compute which timestamps to evict
   */
  private void evictExpiredRequests(Deque<Long> timestamps, long now) {
    long threshold = now - windowMillis;
    while (!timestamps.isEmpty() && timestamps.peekFirst() <= threshold) {
      timestamps.removeFirst();
    }
  }

  /**
   * Derives the client key used for rate limiting from the HTTP request.
   *
   * @param request the incoming HTTP request
   * @return the client's remote IP address as returned by {@code request.getRemoteAddr()}
   */
  private String resolveClientKey(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  /**
   * Clears all tracked per-client request timestamps, resetting the interceptor's rate-limiting state.
   *
   * This invalidates every entry in the internal cache that stores recent request timestamps per client.
   */
  public void reset() {
    requestsByClient.invalidateAll();
  }

  /**
   * Create a cache that maps client keys to deques of request timestamps and evicts entries
   * if they are not accessed within the given window.
   *
   * @param windowSeconds the inactivity window, in seconds, after which a client's entry is evicted
   * @return a Caffeine cache mapping client key strings to Deque<Long> instances of request timestamps
   */
  private static Cache<String, Deque<Long>> buildRequestsByClientCache(long windowSeconds) {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(windowSeconds)).build();
  }
}
