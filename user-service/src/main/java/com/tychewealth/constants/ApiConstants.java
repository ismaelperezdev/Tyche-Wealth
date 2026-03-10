package com.tychewealth.constants;

import org.springframework.http.MediaType;

public class ApiConstants {
  public static final String URL_FOLDER = "/tyche-wealth/user-service";
  public static final String VERSION_1 = "/v1";
  public static final String URL_FOLDER_V1 = URL_FOLDER + VERSION_1;

  public static final String AUTH_BASE_URL = URL_FOLDER_V1 + "/auth";
  public static final String AUTH_REGISTER_URL = AUTH_BASE_URL + "/register";
  public static final String AUTH_LOGIN_URL = AUTH_BASE_URL + "/login";
  public static final String AUTH_REFRESH_URL = AUTH_BASE_URL + "/refresh";

  public static final String REQUEST_PRODUCES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
  public static final String REQUEST_CONSUMES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

  ApiConstants() {}
}
