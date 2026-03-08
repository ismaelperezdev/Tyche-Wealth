package com.tychewealth.constants;

public final class LogConstants {

  public static final String BASE_LOG = "{} {}";

  public static final String AUTH = "[auth]";
  public static final String USER = "[user]";
  public static final String REGISTER_ACTION = "[register]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded";
  public static final String REQUEST_CONFLICT = BASE_LOG + " Request rejected: {}";
  public static final String REGISTER_REQUEST_FIELDS = " username={}, email={}";
  public static final String REGISTER_CREATED_USER_ID = " userId={}";

  private LogConstants() {}
}
