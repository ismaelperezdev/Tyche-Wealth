package com.tychewealth.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthRateLimitInterceptor implements HandlerInterceptor {

  private final int maxRequests;
  private final long windowMillis;
  private final String rejectionMessage;
  private final Consumer<String> requestMetricRecorder;
  private final Consumer<String> rateLimitedMetricRecorder;
  private final Clock clock;
  private final Cache<String, Deque<Long>> requestsByClient;

  /**
   * Create an interceptor that enforces per-client rate limiting using a sliding time window.
   *
   * @param maxRequests               maximum number of requests allowed per client within the sliding window
   * @param windowSeconds             duration of the sliding window in seconds
   * @param rejectionMessage         message returned when a request is rejected due to exceeding the rate limit
   * @param requestMetricRecorder    optional consumer invoked for each request with the request URI (may be null)
   * @param rateLimitedMetricRecorder optional consumer invoked when a request is rejected with the request URI (may be null)
   * @throws IllegalArgumentException if {@code maxRequests} &le; 0 or {@code windowSeconds} &le; 0
   */
  public AuthRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder) {
    this(
        maxRequests,
        windowSeconds,
        rejectionMessage,
        requestMetricRecorder,
        rateLimitedMetricRecorder,
        buildRequestsByClientCache(windowSeconds),
        Clock.systemUTC());
  }

  /**
   * Create an interceptor that enforces per-client rate limits using a sliding time window.
   *
   * @param maxRequests               maximum allowed requests per client within the window; must be greater than 0
   * @param windowSeconds             sliding window length in seconds; must be greater than 0
   * @param rejectionMessage          message used when rejecting requests due to rate limiting
   * @param requestMetricRecorder     optional callback invoked with the request URI for every request (may be null)
   * @param rateLimitedMetricRecorder optional callback invoked with the request URI when a request is rejected (may be null)
   * @param requestsByClient          cache mapping client keys to deques of request timestamps (milliseconds)
   * @param clock                     source of current time used for window calculations
   * @throws IllegalArgumentException if {@code maxRequests} or {@code windowSeconds} is not positive
   */
  AuthRateLimitInterceptor(
      int maxRequests,
      long windowSeconds,
      String rejectionMessage,
      Consumer<String> requestMetricRecorder,
      Consumer<String> rateLimitedMetricRecorder,
      Cache<String, Deque<Long>> requestsByClient,
      Clock clock) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("Rate limit max requests must be positive");
    }
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("Rate limit window must be positive");
    }
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
    this.rejectionMessage = rejectionMessage;
    this.requestMetricRecorder = requestMetricRecorder;
    this.rateLimitedMetricRecorder = rateLimitedMetricRecorder;
    this.requestsByClient = requestsByClient;
    this.clock = clock;
  }

  /**
   * Enforces per-client sliding-window rate limiting for the incoming HTTP request.
   *
   * Records a request metric (if configured), identifies the client, evicts expired timestamps,
   * and either records the current request timestamp or rejects the request when the client
   * has reached the configured limit.
   *
   * @param request  the HTTP request (used to resolve the client key and request URI for metrics)
   * @param response the HTTP response
   * @param handler  the chosen handler to execute, for type or/or instance examination
   * @return true to continue request processing
   * @throws org.springframework.web.server.ResponseStatusException with HTTP 429 (TOO_MANY_REQUESTS)
   *         when the client has exceeded the allowed number of requests in the configured window
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (requestMetricRecorder != null) {
      requestMetricRecorder.accept(request.getRequestURI());
    }

    String clientKey = resolveClientKey(request);
    long now = clock.millis();
    Deque<Long> timestamps = requestsByClient.get(clientKey, ignored -> new ArrayDeque<>());

    synchronized (timestamps) {
      evictExpiredRequests(timestamps, now);
      if (timestamps.size() >= maxRequests) {
        if (rateLimitedMetricRecorder != null) {
          rateLimitedMetricRecorder.accept(request.getRequestURI());
        }
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, rejectionMessage);
      }
      timestamps.addLast(now);
    }

    return true;
  }

  /**
   * Clears all recorded per-client request state so rate limits are reset for every client.
   */
  public void reset() {
    requestsByClient.invalidateAll();
  }

  /**
   * Removes request timestamps from the front of the deque that are older than the current sliding window.
   *
   * @param timestamps deque of request timestamps in milliseconds, ordered from oldest to newest
   * @param now       current time in milliseconds used to compute the eviction threshold
   */
  private void evictExpiredRequests(Deque<Long> timestamps, long now) {
    long threshold = now - windowMillis;
    while (!timestamps.isEmpty() && timestamps.peekFirst() <= threshold) {
      timestamps.removeFirst();
    }
  }

  /**
   * Determine the client identifier used for rate limiting from the HTTP request.
   *
   * @param request the incoming HTTP servlet request
   * @return the client's IP address (the request's remote address)
   */
  private String resolveClientKey(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  /**
   * Create a cache for per-client request timestamp deques that expires entries after a period of inactivity.
   *
   * @param windowSeconds number of seconds of inactivity after which a client's entry is removed
   * @return a cache mapping client keys to deques of request timestamps (milliseconds)
   */
  private static Cache<String, Deque<Long>> buildRequestsByClientCache(long windowSeconds) {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(windowSeconds)).build();
  }
}
