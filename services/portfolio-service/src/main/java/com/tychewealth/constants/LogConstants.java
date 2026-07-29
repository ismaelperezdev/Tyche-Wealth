package com.tychewealth.constants;

public final class LogConstants {

  public static final String BASE_LOG = "{} {}";

  public static final String PORTFOLIO = "[portfolio]";
  public static final String ASSET = "[asset]";
  public static final String AUTH = "[auth]";
  public static final String SYSTEM = "[system]";
  public static final String CREATE_ACTION = "[create]";
  public static final String RETRIEVE_ACTION = "[retrieve]";
  public static final String UPDATE_ACTION = "[update]";
  public static final String DELETE_ACTION = "[delete]";
  public static final String LIST_PORTFOLIOS_ACTION = "[list-portfolios]";
  public static final String LIST_ASSETS_ACTION = "[list-assets]";
  public static final String IMPORT_ASSETS_ACTION = "[import-assets]";
  public static final String ERROR_HANDLER_ACTION = "[error-handler]";
  public static final String ACCESS_TOKEN_ACTION = "[access-token]";
  public static final String SEND_ACTION = "[send]";
  public static final String RATE_LIMIT_ACTION = "[rate-limit]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded";
  public static final String REQUEST_CONFLICT = BASE_LOG + " Request rejected: {}";
  public static final String REQUEST_CONFLICT_WITH_URI = REQUEST_CONFLICT + " uri={}";
  public static final String FILE_NAME_CONTEXT = " fileName={}";
  public static final String MODEL_TYPE_CONTEXT = " modelType={}";
  public static final String PORTFOLIO_NAME = " portfolioName={}";
  public static final String PORTFOLIO_ID = " portfolioId={}";
  public static final String USER_ID = " userId={}";
  public static final String TOPIC = " topic={}";
  public static final String ACTIVE_USERS_EVENT_SUCCESS_CONTEXT = TOPIC + " activeUsers={}";
  public static final String UNHANDLED_EXCEPTION_MESSAGE =
      "unhandled exception reached global error handler";
  public static final String MISSING_AUTHENTICATED_USER_MESSAGE = "missing authenticated user";
  public static final String PORTFOLIO_NAME_ALREADY_EXISTS_MESSAGE =
      "portfolio name already exists for user";
  public static final String PORTFOLIO_LIMIT_REACHED_MESSAGE = "portfolio limit reached for user";
  public static final String PORTFOLIO_NOT_FOUND_MESSAGE = "portfolio not found for user";
  public static final String ASSET_LIMIT_REACHED_MESSAGE = "asset limit reached for portfolio";
  public static final String ASSET_NAME_ALREADY_EXISTS_MESSAGE =
      "asset name already exists for portfolio";
  public static final String ASSET_NOT_FOUND_MESSAGE = "asset not found for portfolio";
  public static final String PORTFOLIO_PERSISTENCE_CONFLICT_MESSAGE =
      "portfolio conflict detected at persistence layer";
  public static final String ASSET_PERSISTENCE_CONFLICT_MESSAGE =
      "asset conflict detected at persistence layer";
  public static final String UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE = "unknown persistence conflict";
  public static final String INVALID_ACCESS_TOKEN_MESSAGE = "invalid access token";
  public static final String INVALID_AUTHORIZATION_HEADER_MESSAGE = "invalid authorization header";
  public static final String REDIS_UNAVAILABLE_MESSAGE =
      "redis unavailable while checking access-token revocation; failing closed";
  public static final String RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT =
      " uri={} namespace={} rejectionMessage={}";
  public static final String RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE = "rate limit store unavailable";
  public static final String IMPORT_QUEUE_STATUS = " fileName={} activeWorkers={} queuedTasks={}";
  public static final String IMPORT_INFLIGHT_ACQUIRED_AND_QUEUED_MESSAGE =
      "asset import inflight lock acquired and queued";
  public static final String IMPORT_COMPLETED_MESSAGE = "asset import completed";
  public static final String IMPORT_INTERRUPTED_MESSAGE = "asset import interrupted while waiting";
  public static final String IMPORT_FAILED_MESSAGE = "asset import failed";
  public static final String IMPORT_PROCESSING_START_MESSAGE = "asset import processing started";
  public static final String IMPORT_PROCESSING_SUCCESS_CONTEXT =
      " fileName={} elapsedMs={} extractedChars={}";
  public static final String IMPORT_PROCESSING_SUCCESS_MESSAGE =
      "asset import processing completed";
  public static final String IMPORT_EXTRACTION_IO_FAILURE_MESSAGE =
      "asset import extraction io failure";
  public static final String IMPORT_QUEUE_FULL_WAIT_MESSAGE =
      "asset import queue full, waiting for available slot";
  public static final String IMPORT_INFLIGHT_WAIT_MESSAGE =
      "asset import already in progress for same file, waiting for cached result";
  public static final String IMPORT_INFLIGHT_RELEASED_MESSAGE =
      "asset import inflight lock released";
  public static final String AI_QUEUE_STATUS = " modelType={} activeWorkers={} queuedTasks={}";
  public static final String AI_REQUEST_QUEUED_MESSAGE = "ai request queued";
  public static final String AI_REQUEST_COMPLETED_MESSAGE = "ai request completed";
  public static final String AI_REQUEST_INTERRUPTED_MESSAGE =
      "ai request interrupted while waiting";
  public static final String AI_REQUEST_FAILED_MESSAGE = "ai request failed";
  public static final String AI_PROCESSING_START_MESSAGE = "ai request processing started";
  public static final String AI_PROCESSING_SUCCESS_CONTEXT =
      " modelType={} elapsedMs={} responseChars={}";
  public static final String AI_PROCESSING_SUCCESS_MESSAGE = "ai request processing completed";
  public static final String AI_QUEUE_FULL_WAIT_MESSAGE =
      "ai queue full, waiting for available slot";

  private LogConstants() {}
}
