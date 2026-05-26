package com.tychewealth.constants;

public final class LogConstants {

  private LogConstants() {}

  public static final String BASE_LOG = "{} {}";

  public static final String RESOURCES_CLIENT = "[resources-client]";
  public static final String VEHICLE_POLLING_SCHEDULER = "[vehicle-polling-scheduler]";
  public static final String ERROR_HANDLER = "[error-handler]";
  public static final String FETCH_ACTION = "[fetch]";
  public static final String POLL_ACTION = "[poll]";
  public static final String HANDLE_ACTION = "[handle]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded count={}";
  public static final String REQUEST_FAILURE = BASE_LOG + " Request failed";
  public static final String POLLING_SUCCESS = BASE_LOG + " Polling completed added={} removed={}";
  public static final String POLLING_FAILURE = BASE_LOG + " Polling failed";
  public static final String POLLING_NO_CHANGES = BASE_LOG + " No vehicle changes detected";
  public static final String POLLING_DURATION = BASE_LOG + " Polling finished durationMs={}";
  public static final String UNHANDLED_EXCEPTION = BASE_LOG + " Unhandled exception";
  public static final String RETRY_ATTEMPT = " retryAttempt={}";
  public static final String CAUSE = " cause={}";
  public static final String REQUEST_RETRY = BASE_LOG + " Request retrying" + RETRY_ATTEMPT + CAUSE;
}
