package com.tychewealth.service.monitoring;

import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_FAILURE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_INVALID_CREDENTIALS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_FAILURE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_TOKEN_ISSUED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_TOKEN_REVOKED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_CONFLICT;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_FAILURE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_SUCCESS;

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
    this.registerRequests =
        counter(
            meterRegistry,
            METRIC_AUTH_REGISTER_REQUESTS,
            "Total register requests received by the auth flow.");
    this.registerSuccesses =
        counter(
            meterRegistry,
            METRIC_AUTH_REGISTER_SUCCESS,
            "Successful user registrations completed by the auth flow.");
    this.registerFailures =
        counter(
            meterRegistry,
            METRIC_AUTH_REGISTER_FAILURE,
            "Failed user registration attempts recorded by the auth flow.");
    this.registerRateLimited =
        counter(
            meterRegistry,
            METRIC_AUTH_REGISTER_RATE_LIMITED,
            "Register requests rejected by rate limiting.");
    this.registerConflicts =
        counter(
            meterRegistry,
            METRIC_AUTH_REGISTER_CONFLICT,
            "Register requests rejected because the email or username already exists.");

    this.loginRequests =
        counter(
            meterRegistry,
            METRIC_AUTH_LOGIN_REQUESTS,
            "Total login requests received by the auth flow.");
    this.loginSuccesses =
        counter(
            meterRegistry,
            METRIC_AUTH_LOGIN_SUCCESS,
            "Successful login requests that issued fresh credentials.");
    this.loginFailures =
        counter(
            meterRegistry,
            METRIC_AUTH_LOGIN_FAILURE,
            "Failed login attempts recorded by the auth flow.");
    this.loginRateLimited =
        counter(
            meterRegistry,
            METRIC_AUTH_LOGIN_RATE_LIMITED,
            "Login requests rejected by rate limiting.");
    this.loginInvalidCredentials =
        counter(
            meterRegistry,
            METRIC_AUTH_LOGIN_INVALID_CREDENTIALS,
            "Login attempts rejected because the provided credentials were invalid.");

    this.refreshRequests =
        counter(
            meterRegistry,
            METRIC_AUTH_REFRESH_REQUESTS,
            "Total refresh-token requests received by the auth flow.");
    this.refreshSuccesses =
        counter(
            meterRegistry,
            METRIC_AUTH_REFRESH_SUCCESS,
            "Successful refresh-token operations that returned fresh credentials.");
    this.refreshFailures =
        counter(
            meterRegistry,
            METRIC_AUTH_REFRESH_FAILURE,
            "Failed refresh-token attempts recorded by the auth flow.");
    this.refreshRateLimited =
        counter(
            meterRegistry,
            METRIC_AUTH_REFRESH_RATE_LIMITED,
            "Refresh-token requests rejected by rate limiting.");
    this.refreshTokensIssued =
        counter(
            meterRegistry,
            METRIC_AUTH_REFRESH_TOKEN_ISSUED,
            "Refresh tokens persisted by login or token rotation flows.");
    this.refreshTokensRevoked =
        counter(
            meterRegistry,
            METRIC_AUTH_REFRESH_TOKEN_REVOKED,
            "Refresh tokens revoked by logout, password changes, soft delete, or token rotation.");
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

  private Counter counter(MeterRegistry meterRegistry, String name, String description) {
    return Counter.builder(name).description(description).register(meterRegistry);
  }
}
