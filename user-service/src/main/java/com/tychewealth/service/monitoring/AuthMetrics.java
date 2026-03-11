package com.tychewealth.service.monitoring;

import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_FAILURE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_INVALID_CREDENTIALS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_RATE_LIMITED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_REQUESTS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_SUCCESS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_FAILURE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_RATE_LIMITED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_REQUESTS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_SUCCESS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_TOKEN_ISSUED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_TOKEN_REVOKED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_CONFLICT;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_FAILURE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_RATE_LIMITED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_REQUESTS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_SUCCESS;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

  private final Counter registerRequests;
  private final Counter registerSuccesses;
  private final Counter registerFailures;
  private final Counter registerRateLimited;
  private final Counter registerConflicts;

  private final Counter loginRequests;
  private final Counter loginSuccesses;
  private final Counter loginFailures;
  private final Counter loginRateLimited;
  private final Counter loginInvalidCredentials;

  private final Counter refreshRequests;
  private final Counter refreshSuccesses;
  private final Counter refreshFailures;
  private final Counter refreshRateLimited;
  private final Counter refreshTokensIssued;
  private final Counter refreshTokensRevoked;

  /**
   * Constructs an AuthMetrics instance and registers Micrometer counters for authentication-related events.
   *
   * @param meterRegistry the MeterRegistry used to obtain counters for registration, login, and token refresh metrics
   */
  public AuthMetrics(MeterRegistry meterRegistry) {
    this.registerRequests = meterRegistry.counter(METRIC_AUTH_REGISTER_REQUESTS);
    this.registerSuccesses = meterRegistry.counter(METRIC_AUTH_REGISTER_SUCCESS);
    this.registerFailures = meterRegistry.counter(METRIC_AUTH_REGISTER_FAILURE);
    this.registerRateLimited = meterRegistry.counter(METRIC_AUTH_REGISTER_RATE_LIMITED);
    this.registerConflicts = meterRegistry.counter(METRIC_AUTH_REGISTER_CONFLICT);

    this.loginRequests = meterRegistry.counter(METRIC_AUTH_LOGIN_REQUESTS);
    this.loginSuccesses = meterRegistry.counter(METRIC_AUTH_LOGIN_SUCCESS);
    this.loginFailures = meterRegistry.counter(METRIC_AUTH_LOGIN_FAILURE);
    this.loginRateLimited = meterRegistry.counter(METRIC_AUTH_LOGIN_RATE_LIMITED);
    this.loginInvalidCredentials = meterRegistry.counter(METRIC_AUTH_LOGIN_INVALID_CREDENTIALS);

    this.refreshRequests = meterRegistry.counter(METRIC_AUTH_REFRESH_REQUESTS);
    this.refreshSuccesses = meterRegistry.counter(METRIC_AUTH_REFRESH_SUCCESS);
    this.refreshFailures = meterRegistry.counter(METRIC_AUTH_REFRESH_FAILURE);
    this.refreshRateLimited = meterRegistry.counter(METRIC_AUTH_REFRESH_RATE_LIMITED);
    this.refreshTokensIssued = meterRegistry.counter(METRIC_AUTH_REFRESH_TOKEN_ISSUED);
    this.refreshTokensRevoked = meterRegistry.counter(METRIC_AUTH_REFRESH_TOKEN_REVOKED);
  }

  /**
   * Record that a registration request occurred by incrementing the registration-requests metric.
   */
  public void recordRegisterRequest() {
    registerRequests.increment();
  }

  /**
   * Increments the metric counter for successful user registration events.
   */
  public void recordRegisterSuccess() {
    registerSuccesses.increment();
  }

  /**
   * Increment the counter tracking failed user registration attempts.
   */
  public void recordRegisterFailure() {
    registerFailures.increment();
  }

  /**
   * Records that a registration attempt was rate limited by incrementing the registration rate-limited metric.
   */
  public void recordRegisterRateLimited() {
    registerRateLimited.increment();
  }

  /**
   * Record that a registration attempt failed because of a conflict.
   *
   * Increments the register-conflict metric counter.
   */
  public void recordRegisterConflict() {
    registerConflicts.increment();
  }

  /**
   * Record that a login request occurred.
   *
   * Increments the counter tracking login request attempts.
   */
  public void recordLoginRequest() {
    loginRequests.increment();
  }

  /**
   * Record a successful user login by incrementing the login-success metric counter.
   */
  public void recordLoginSuccess() {
    loginSuccesses.increment();
  }

  /**
   * Record a failed login attempt for authentication metrics.
   *
   * Increments the counter that tracks login failures.
   */
  public void recordLoginFailure() {
    loginFailures.increment();
  }

  /**
   * Record that a login attempt was rate limited by incrementing the login rate-limited metric.
   */
  public void recordLoginRateLimited() {
    loginRateLimited.increment();
  }

  /**
   * Record a login attempt that failed due to invalid credentials.
   *
   * Increments the counter tracking login attempts rejected for invalid credentials.
   */
  public void recordLoginInvalidCredentials() {
    loginInvalidCredentials.increment();
  }

  /**
   * Records an authentication token refresh request metric.
   */
  public void recordRefreshRequest() {
    refreshRequests.increment();
  }

  /**
   * Record a successful token refresh.
   *
   * Increments the counter for refresh successes.
   */
  public void recordRefreshSuccess() {
    refreshSuccesses.increment();
  }

  /**
   * Increments the metric counter for refresh operation failures.
   */
  public void recordRefreshFailure() {
    refreshFailures.increment();
  }

  /**
   * Record that a token refresh request was rate limited.
   */
  public void recordRefreshRateLimited() {
    refreshRateLimited.increment();
  }

  /**
   * Record issuance of refresh tokens.
   *
   * Increments the refresh token-issued counter by the specified positive count.
   *
   * @param count the number of tokens issued; only values greater than zero are recorded
   */
  public void recordTokensIssued(double count) {
    if (count > 0) {
      refreshTokensIssued.increment(count);
    }
  }

  /**
   * Record that a number of refresh tokens were revoked.
   *
   * @param count the number of tokens revoked; only values greater than zero will be recorded
   */
  public void recordTokensRevoked(double count) {
    if (count > 0) {
      refreshTokensRevoked.increment(count);
    }
  }
}
