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

  public void recordRegisterRequest() {
    registerRequests.increment();
  }

  public void recordRegisterSuccess() {
    registerSuccesses.increment();
  }

  public void recordRegisterFailure() {
    registerFailures.increment();
  }

  public void recordRegisterRateLimited() {
    registerRateLimited.increment();
  }

  public void recordRegisterConflict() {
    registerConflicts.increment();
  }

  public void recordLoginRequest() {
    loginRequests.increment();
  }

  public void recordLoginSuccess() {
    loginSuccesses.increment();
  }

  public void recordLoginFailure() {
    loginFailures.increment();
  }

  public void recordLoginRateLimited() {
    loginRateLimited.increment();
  }

  public void recordLoginInvalidCredentials() {
    loginInvalidCredentials.increment();
  }

  public void recordRefreshRequest() {
    refreshRequests.increment();
  }

  public void recordRefreshSuccess() {
    refreshSuccesses.increment();
  }

  public void recordRefreshFailure() {
    refreshFailures.increment();
  }

  public void recordRefreshRateLimited() {
    refreshRateLimited.increment();
  }

  public void recordTokensIssued(double count) {
    if (count > 0) {
      refreshTokensIssued.increment(count);
    }
  }

  public void recordTokensRevoked(double count) {
    if (count > 0) {
      refreshTokensRevoked.increment(count);
    }
  }
}
