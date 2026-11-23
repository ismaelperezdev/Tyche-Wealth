package com.tychewealth.serviceauth.constants;

public final class LogConstants {

  private LogConstants() {}

  public static final String BASE_LOG = "{} {}";

  public static final String SERVICE_AUTH = "[service-auth]";
  public static final String ACCESS_TOKEN_ACTION = "[access-token]";

  public static final String AUTHENTICATION_FAILURE =
      BASE_LOG + " Authorization failed errorCode={}";
  public static final String AUTHENTICATION_SUCCESS = BASE_LOG + " Authorization succeeded";
  public static final String NO_AUTHORIZED_CLIENT = BASE_LOG + " Authorization returned no client";
}
