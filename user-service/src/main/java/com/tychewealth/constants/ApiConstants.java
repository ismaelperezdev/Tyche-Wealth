package com.tychewealth.constants;

import com.tychewealth.utils.LogContextFactory;
import org.springframework.http.MediaType;

public class ApiConstants {
  public static final String URL_FOLDER = "/tyche-wealth/user-service";
  public static final String VERSION_1 = "/v1";
  public static final String URL_FOLDER_V1 = URL_FOLDER + VERSION_1;

  public static final String REQUEST_PRODUCES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
  public static final String REQUEST_CONSUMES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

  public static final String REGISTER_ACTION = LogContextFactory.action("register");

  ApiConstants() {}
}
