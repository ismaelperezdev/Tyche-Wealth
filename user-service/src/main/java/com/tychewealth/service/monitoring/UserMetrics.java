package com.tychewealth.service.monitoring;

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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UserMetrics {

  private final Counter retrieveRequests;
  private final Counter retrieveSuccesses;
  private final Counter updateRequests;
  private final Counter updateSuccesses;
  private final Counter updatePasswordRequests;
  private final Counter updatePasswordSuccesses;
  private final Counter deleteRequests;
  private final Counter deleteSuccesses;
  private final Counter unauthorized;
  private final Counter notFound;
  private final Counter usernameConflict;
  private final Counter currentPasswordInvalid;
  private final Counter newPasswordReused;

  public UserMetrics(MeterRegistry meterRegistry) {
    this.retrieveRequests =
        counter(
            meterRegistry,
            METRIC_USER_RETRIEVE_REQUESTS,
            "Total authenticated user profile retrieval requests.");
    this.retrieveSuccesses =
        counter(
            meterRegistry,
            METRIC_USER_RETRIEVE_SUCCESS,
            "Successful authenticated user profile retrievals.");
    this.updateRequests =
        counter(
            meterRegistry,
            METRIC_USER_UPDATE_REQUESTS,
            "Total authenticated user profile update requests.");
    this.updateSuccesses =
        counter(
            meterRegistry,
            METRIC_USER_UPDATE_SUCCESS,
            "Successful authenticated user profile updates.");
    this.updatePasswordRequests =
        counter(
            meterRegistry,
            METRIC_USER_UPDATE_PASSWORD_REQUESTS,
            "Total authenticated password change requests.");
    this.updatePasswordSuccesses =
        counter(
            meterRegistry,
            METRIC_USER_UPDATE_PASSWORD_SUCCESS,
            "Successful authenticated password changes.");
    this.deleteRequests =
        counter(
            meterRegistry,
            METRIC_USER_DELETE_REQUESTS,
            "Total authenticated account deletion requests.");
    this.deleteSuccesses =
        counter(
            meterRegistry,
            METRIC_USER_DELETE_SUCCESS,
            "Successful authenticated account soft deletions.");
    this.unauthorized =
        counter(
            meterRegistry,
            METRIC_USER_UNAUTHORIZED,
            "User-area requests rejected because authentication was missing or invalid.");
    this.notFound =
        counter(
            meterRegistry,
            METRIC_USER_NOT_FOUND,
            "User-area operations that targeted a user record not found as active.");
    this.usernameConflict =
        counter(
            meterRegistry,
            METRIC_USER_USERNAME_CONFLICT,
            "User update requests rejected because the requested username was already in use.");
    this.currentPasswordInvalid =
        counter(
            meterRegistry,
            METRIC_USER_CURRENT_PASSWORD_INVALID,
            "Password change requests rejected because the current password did not match.");
    this.newPasswordReused =
        counter(
            meterRegistry,
            METRIC_USER_NEW_PASSWORD_REUSED,
            "Password change requests rejected because the new password matched the current password.");
  }

  public void recordRetrieveRequest() {
    retrieveRequests.increment();
  }

  public void recordRetrieveSuccess() {
    retrieveSuccesses.increment();
  }

  public void recordUpdateRequest() {
    updateRequests.increment();
  }

  public void recordUpdateSuccess() {
    updateSuccesses.increment();
  }

  public void recordUpdatePasswordRequest() {
    updatePasswordRequests.increment();
  }

  public void recordUpdatePasswordSuccess() {
    updatePasswordSuccesses.increment();
  }

  public void recordDeleteRequest() {
    deleteRequests.increment();
  }

  public void recordDeleteSuccess() {
    deleteSuccesses.increment();
  }

  public void recordUnauthorized() {
    unauthorized.increment();
  }

  public void recordNotFound() {
    notFound.increment();
  }

  public void recordUsernameConflict() {
    usernameConflict.increment();
  }

  public void recordCurrentPasswordInvalid() {
    currentPasswordInvalid.increment();
  }

  public void recordNewPasswordReused() {
    newPasswordReused.increment();
  }

  private Counter counter(MeterRegistry meterRegistry, String name, String description) {
    return Counter.builder(name).description(description).register(meterRegistry);
  }
}
