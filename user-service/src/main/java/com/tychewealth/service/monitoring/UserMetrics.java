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
    this.retrieveRequests = meterRegistry.counter(METRIC_USER_RETRIEVE_REQUESTS);
    this.retrieveSuccesses = meterRegistry.counter(METRIC_USER_RETRIEVE_SUCCESS);
    this.updateRequests = meterRegistry.counter(METRIC_USER_UPDATE_REQUESTS);
    this.updateSuccesses = meterRegistry.counter(METRIC_USER_UPDATE_SUCCESS);
    this.updatePasswordRequests = meterRegistry.counter(METRIC_USER_UPDATE_PASSWORD_REQUESTS);
    this.updatePasswordSuccesses = meterRegistry.counter(METRIC_USER_UPDATE_PASSWORD_SUCCESS);
    this.deleteRequests = meterRegistry.counter(METRIC_USER_DELETE_REQUESTS);
    this.deleteSuccesses = meterRegistry.counter(METRIC_USER_DELETE_SUCCESS);
    this.unauthorized = meterRegistry.counter(METRIC_USER_UNAUTHORIZED);
    this.notFound = meterRegistry.counter(METRIC_USER_NOT_FOUND);
    this.usernameConflict = meterRegistry.counter(METRIC_USER_USERNAME_CONFLICT);
    this.currentPasswordInvalid = meterRegistry.counter(METRIC_USER_CURRENT_PASSWORD_INVALID);
    this.newPasswordReused = meterRegistry.counter(METRIC_USER_NEW_PASSWORD_REUSED);
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
}
