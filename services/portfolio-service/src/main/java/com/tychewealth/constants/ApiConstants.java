package com.tychewealth.constants;

import org.springframework.http.MediaType;

public final class ApiConstants {

  public static final String URL_FOLDER = "/tyche-wealth/portfolio-service";
  public static final String VERSION_1 = "/v1";
  public static final String URL_FOLDER_V1 = URL_FOLDER + VERSION_1;
  public static final String PORTFOLIO_BASE_URL = URL_FOLDER_V1 + "/portfolio";

  public static final String REQUEST_PRODUCES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
  public static final String REQUEST_CONSUMES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

  private ApiConstants() {}
}
