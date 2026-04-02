package com.tychewealth.enums;

import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_FAILURE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_INVALID_CREDENTIALS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_RATE_LIMIT_STORE_UNAVAILABLE;
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
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_RATE_LIMIT_STORE_UNAVAILABLE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_TOKEN_STATE_UNAVAILABLE;

public enum AuthMetricEnum {
  REGISTER_REQUESTS(
      METRIC_AUTH_REGISTER_REQUESTS, "Total register requests received by the auth flow."),
  REGISTER_SUCCESS(
      METRIC_AUTH_REGISTER_SUCCESS, "Successful user registrations completed by the auth flow."),
  REGISTER_FAILURE(
      METRIC_AUTH_REGISTER_FAILURE, "Failed user registration attempts recorded by the auth flow."),
  REGISTER_RATE_LIMITED(
      METRIC_AUTH_REGISTER_RATE_LIMITED, "Register requests rejected by rate limiting."),
  REGISTER_RATE_LIMIT_STORE_UNAVAILABLE(
      METRIC_AUTH_REGISTER_RATE_LIMIT_STORE_UNAVAILABLE,
      "Register requests that could not be rate-limited because the backing store was unavailable."),
  REGISTER_CONFLICT(
      METRIC_AUTH_REGISTER_CONFLICT,
      "Register requests rejected because the email or username already exists."),
  LOGIN_REQUESTS(METRIC_AUTH_LOGIN_REQUESTS, "Total login requests received by the auth flow."),
  LOGIN_SUCCESS(
      METRIC_AUTH_LOGIN_SUCCESS, "Successful login requests that issued fresh credentials."),
  LOGIN_FAILURE(METRIC_AUTH_LOGIN_FAILURE, "Failed login attempts recorded by the auth flow."),
  LOGIN_RATE_LIMITED(METRIC_AUTH_LOGIN_RATE_LIMITED, "Login requests rejected by rate limiting."),
  LOGIN_RATE_LIMIT_STORE_UNAVAILABLE(
      METRIC_AUTH_LOGIN_RATE_LIMIT_STORE_UNAVAILABLE,
      "Login requests that could not be rate-limited because the backing store was unavailable."),
  LOGIN_INVALID_CREDENTIALS(
      METRIC_AUTH_LOGIN_INVALID_CREDENTIALS,
      "Login attempts rejected because the provided credentials were invalid."),
  TOKEN_STATE_UNAVAILABLE(
      METRIC_AUTH_TOKEN_STATE_UNAVAILABLE,
      "Token-state checks that could not reach Redis and therefore failed closed."),
  REFRESH_REQUESTS(
      METRIC_AUTH_REFRESH_REQUESTS, "Total refresh-token requests received by the auth flow."),
  REFRESH_SUCCESS(
      METRIC_AUTH_REFRESH_SUCCESS,
      "Successful refresh-token operations that returned fresh credentials."),
  REFRESH_FAILURE(
      METRIC_AUTH_REFRESH_FAILURE, "Failed refresh-token attempts recorded by the auth flow."),
  REFRESH_RATE_LIMITED(
      METRIC_AUTH_REFRESH_RATE_LIMITED, "Refresh-token requests rejected by rate limiting."),
  REFRESH_TOKEN_ISSUED(
      METRIC_AUTH_REFRESH_TOKEN_ISSUED,
      "Refresh tokens persisted by login or token rotation flows."),
  REFRESH_TOKEN_REVOKED(
      METRIC_AUTH_REFRESH_TOKEN_REVOKED,
      "Refresh tokens revoked by logout, password changes, soft delete, or token rotation.");

  private final String metricName;
  private final String description;

  AuthMetricEnum(String metricName, String description) {
    this.metricName = metricName;
    this.description = description;
  }

  public String metricName() {
    return metricName;
  }

  public String description() {
    return description;
  }
}
