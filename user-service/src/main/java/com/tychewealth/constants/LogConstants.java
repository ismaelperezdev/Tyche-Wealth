package com.tychewealth.constants;

public final class LogConstants {

  public static final String BASE_LOG = "{} {}";

  public static final String AUTH = "[auth]";
  public static final String USER = "[user]";
  public static final String REGISTER_ACTION = "[register]";
  public static final String LOGIN_ACTION = "[login]";
  public static final String REFRESH_TOKEN_ACTION = "[refresh-token]";
  public static final String LOGOUT_ACTION = "[logout]";
  public static final String RETRIEVE_ACTION = "[retrieve]";
  public static final String UPDATE_ACTION = "[update]";
  public static final String DELETE_ACTION = "[delete]";
  public static final String ACCESS_TOKEN_ACTION = "[access-token]";

  public static final String REQUEST_START = BASE_LOG + " Request started";
  public static final String REQUEST_SUCCESS = BASE_LOG + " Request succeeded";
  public static final String REQUEST_CONFLICT = BASE_LOG + " Request rejected: {}";
  public static final String REGISTER_REQUEST_FIELDS = " username={}, email={}";
  public static final String LOGIN_REQUEST_FIELDS = " email={}";
  public static final String UPDATE_REQUEST_FIELDS = " username={}";
  public static final String REGISTER_CREATED_USER_ID = " userId={}";
  public static final String LOGIN_USER_ID = " userId={}";
  public static final String LOGOUT_USER_ID = " userId={}";
  public static final String RETRIEVE_USER_ID = " userId={}";
  public static final String UPDATE_USER_ID = " userId={}";
  public static final String DELETE_USER_ID = " userId={}";
  public static final String INVALID_REFRESH_TOKEN_MESSAGE = "invalid refresh token";
  public static final String INVALID_AUTHORIZATION_HEADER_MESSAGE = "invalid authorization header";
  public static final String INVALID_ACCESS_TOKEN_MESSAGE = "invalid access token";

  private LogConstants() {}
}
