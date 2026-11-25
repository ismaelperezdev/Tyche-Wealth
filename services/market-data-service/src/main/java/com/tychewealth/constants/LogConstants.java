package com.tychewealth.constants;

public final class LogConstants {

  private LogConstants() {}

  public static final String BASE_LOG = "{} {}";

  public static final String TWELVE_DATA_CLIENT = "[twelve-data-client]";
  public static final String MARKET_QUOTE_SCHEDULER = "[market-quote-scheduler]";
  public static final String EXCHANGE_RATE_INGESTION_SCHEDULER =
      "[exchange-rate-ingestion-scheduler]";
  public static final String ACTIVE_SYMBOL_CHANGES_EVENT = "[active-symbol-changes-event]";
  public static final String ERROR_HANDLER = "[error-handler]";
  public static final String SECURITY = "[security]";
  public static final String FETCH_ACTION = "[fetch]";
  public static final String QUOTE_CAPTURE_ACTION = "[quote-capture]";
  public static final String EXCHANGE_RATE_INGESTION_ACTION = "[exchange-rate-ingestion]";
  public static final String HANDLE_ACTION = "[handle]";
  public static final String PRICE_ACTION = "[price]";
  public static final String QUOTE_ACTION = "[quote]";
  public static final String TIME_SERIES_ACTION = "[time-series]";
  public static final String SYNC_ACTION = "[sync]";
  public static final String AUTHENTICATION_ACTION = "[authentication]";
  public static final String AUTHORIZATION_ACTION = "[authorization]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded count={}";
  public static final String REQUEST_FAILURE = BASE_LOG + " Request failed";
  public static final String AUTHENTICATION_FAILURE = BASE_LOG + " Request rejected: {}";
  public static final String AUTHENTICATION_SERVICE_FAILURE =
      BASE_LOG + " Authentication service unavailable";
  public static final String AUTHORIZATION_FAILURE = BASE_LOG + " Request rejected";
  public static final String AUTHORIZATION_SUCCESS = BASE_LOG + " Request authorized";
  public static final String POLLING_SUCCESS = BASE_LOG + " Polling completed added={} removed={}";
  public static final String QUOTE_CAPTURE_SUCCESS =
      BASE_LOG + " Quote capture completed durationMs={}";
  public static final String QUOTE_CAPTURE_FAILURE = BASE_LOG + " Quote capture failed";
  public static final String EXCHANGE_RATE_INGESTION_SUCCESS =
      BASE_LOG + " Exchange-rate ingestion completed count={} durationMs={}";
  public static final String EXCHANGE_RATE_INGESTION_FAILURE =
      BASE_LOG + " Exchange-rate ingestion failed";
  public static final String UNHANDLED_EXCEPTION = BASE_LOG + " Unhandled exception";
  public static final String RETRY_ATTEMPT = " retryAttempt={}";
  public static final String CAUSE = " cause={}";
  public static final String REQUEST_RETRY = BASE_LOG + " Request retrying" + RETRY_ATTEMPT + CAUSE;
}
