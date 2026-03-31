package com.tychewealth.enums;

import static com.tychewealth.constants.MetricConstants.METRIC_USER_CURRENT_PASSWORD_INVALID;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_DELETE_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_DELETE_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_NEW_PASSWORD_REUSED;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_NOT_FOUND;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_RETRIEVE_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_RETRIEVE_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_UNAUTHORIZED;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_UPDATE_PASSWORD_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_UPDATE_PASSWORD_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_UPDATE_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_UPDATE_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_USER_USERNAME_CONFLICT;

public enum UserMetricEnum {
  RETRIEVE_REQUESTS(
      METRIC_USER_RETRIEVE_REQUESTS, "Total authenticated user profile retrieval requests."),
  RETRIEVE_SUCCESS(
      METRIC_USER_RETRIEVE_SUCCESS, "Successful authenticated user profile retrievals."),
  UPDATE_REQUESTS(METRIC_USER_UPDATE_REQUESTS, "Total authenticated user profile update requests."),
  UPDATE_SUCCESS(METRIC_USER_UPDATE_SUCCESS, "Successful authenticated user profile updates."),
  UPDATE_PASSWORD_REQUESTS(
      METRIC_USER_UPDATE_PASSWORD_REQUESTS, "Total authenticated password change requests."),
  UPDATE_PASSWORD_SUCCESS(
      METRIC_USER_UPDATE_PASSWORD_SUCCESS, "Successful authenticated password changes."),
  DELETE_REQUESTS(METRIC_USER_DELETE_REQUESTS, "Total authenticated account deletion requests."),
  DELETE_SUCCESS(METRIC_USER_DELETE_SUCCESS, "Successful authenticated account soft deletions."),
  UNAUTHORIZED(
      METRIC_USER_UNAUTHORIZED,
      "User-area requests rejected because authentication was missing or invalid."),
  NOT_FOUND(
      METRIC_USER_NOT_FOUND,
      "User-area operations that targeted a user record not found as active."),
  USERNAME_CONFLICT(
      METRIC_USER_USERNAME_CONFLICT,
      "User update requests rejected because the requested username was already in use."),
  CURRENT_PASSWORD_INVALID(
      METRIC_USER_CURRENT_PASSWORD_INVALID,
      "Password change requests rejected because the current password did not match."),
  NEW_PASSWORD_REUSED(
      METRIC_USER_NEW_PASSWORD_REUSED,
      "Password change requests rejected because the new password matched the current password.");

  private final String metricName;
  private final String description;

  UserMetricEnum(String metricName, String description) {
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
