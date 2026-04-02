package com.tychewealth.constants;

public final class LogConstants {

  public static final String BASE_LOG = "{} {}";

  public static final String PORTFOLIO = "[portfolio]";
  public static final String AUTH = "[auth]";
  public static final String SYSTEM = "[system]";
  public static final String CREATE_ACTION = "[create]";
  public static final String ERROR_HANDLER_ACTION = "[error-handler]";
  public static final String ACCESS_TOKEN_ACTION = "[access-token]";
  public static final String RATE_LIMIT_ACTION = "[rate-limit]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded";
  public static final String REQUEST_CONFLICT = BASE_LOG + " Request rejected: {}";
  public static final String REQUEST_CONFLICT_WITH_URI = REQUEST_CONFLICT + " uri={}";
  public static final String PORTFOLIO_NAME = " portfolioName={}";
  public static final String USER_ID = " userId={}";
  public static final String UNHANDLED_EXCEPTION_MESSAGE =
      "unhandled exception reached global error handler";
  public static final String MISSING_AUTHENTICATED_USER_MESSAGE = "missing authenticated user";
  public static final String PORTFOLIO_NAME_ALREADY_EXISTS_MESSAGE =
      "portfolio name already exists for user";
  public static final String PORTFOLIO_PERSISTENCE_CONFLICT_MESSAGE =
      "portfolio conflict detected at persistence layer";
  public static final String UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE = "unknown persistence conflict";
  public static final String INVALID_ACCESS_TOKEN_MESSAGE = "invalid access token";
  public static final String INVALID_AUTHORIZATION_HEADER_MESSAGE = "invalid authorization header";
  public static final String REDIS_UNAVAILABLE_MESSAGE =
      "redis unavailable while checking access-token revocation; failing closed";
  public static final String RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT =
      " uri={} namespace={} rejectionMessage={}";
  public static final String RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE = "rate limit store unavailable";

  private LogConstants() {}
}
