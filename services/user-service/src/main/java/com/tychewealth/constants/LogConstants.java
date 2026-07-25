package com.tychewealth.constants;

public final class LogConstants {

  public static final String BASE_LOG = "{} {}";

  public static final String AUTH = "[auth]";
  public static final String EMAIL = "[email]";
  public static final String SYSTEM = "[system]";
  public static final String USER = "[user]";
  public static final String REGISTER_ACTION = "[register]";
  public static final String LOGIN_ACTION = "[login]";
  public static final String VERIFY_REGISTRATION_ACTION = "[verify-registration]";
  public static final String VERIFY_LOGIN_DEVICE_ACTION = "[verify-login-device]";
  public static final String FORGOT_PASSWORD_ACTION = "[forgot-password]";
  public static final String REFRESH_TOKEN_ACTION = "[refresh-token]";
  public static final String RATE_LIMIT_ACTION = "[rate-limit]";
  public static final String LOGOUT_ACTION = "[logout]";
  public static final String RETRIEVE_ACTION = "[retrieve]";
  public static final String UPDATE_ACTION = "[update]";
  public static final String UPDATE_PASSWORD_ACTION = "[update-password]";
  public static final String DELETE_ACTION = "[delete]";
  public static final String ACCESS_TOKEN_ACTION = "[access-token]";
  public static final String ERROR_HANDLER_ACTION = "[error-handler]";
  public static final String SEND_ACTION = "[send]";
  public static final String ACTIVE_USER_SNAPSHOT_ACTION = "[active-user-snapshot]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded";
  public static final String REQUEST_CONFLICT = BASE_LOG + " Request rejected: {}";
  public static final String REQUEST_CONFLICT_WITH_URI = REQUEST_CONFLICT + " uri={}";
  public static final String RATE_LIMIT_STORE_UNAVAILABLE_CONTEXT =
      " uri={} namespace={} rejectionMessage={}";
  public static final String REGISTER_REQUEST_FIELDS = " username={}, email={}";
  public static final String LOGIN_REQUEST_FIELDS = " email={}";
  public static final String UPDATE_REQUEST_FIELDS = " username={}";
  public static final String USER_ID = " userId={}";
  public static final String TOPIC = " topic={}";
  public static final String ACTIVE_USERS_EVENT_SUCCESS_CONTEXT = TOPIC + " activeUsers={}";

  public static final String INVALID_LOGIN_CREDENTIALS_MESSAGE = "invalid login credentials";
  public static final String INVALID_PASSWORD_FORMAT_MESSAGE = "invalid password format";
  public static final String INVALID_REFRESH_TOKEN_MESSAGE = "invalid refresh token";
  public static final String INVALID_AUTHORIZATION_HEADER_MESSAGE = "invalid authorization header";
  public static final String INVALID_ACCESS_TOKEN_MESSAGE = "invalid access token";
  public static final String UNHANDLED_EXCEPTION_MESSAGE =
      "unhandled exception reached global error handler";
  public static final String RATE_LIMIT_STORE_UNAVAILABLE_MESSAGE = "rate limit store unavailable";
  public static final String RESEND_DELIVERY_FAILED_MESSAGE = "resend delivery failed";
  public static final String EMAIL_DAILY_QUOTA_SKIPPED_MESSAGE =
      "daily email quota exceeded; skipping send";
  public static final String ACTIVE_USER_SNAPSHOT_SUCCESS_CONTEXT = " activeUsers={} durationMs={}";
  public static final String ACTIVE_USER_SNAPSHOT_FAILURE_MESSAGE =
      "active user snapshot refresh failed";
  public static final String ACTIVE_USERS_EVENT_PUBLISH_FAILURE_MESSAGE =
      "active users event publishing failed";

  private LogConstants() {}
}
