package com.tychewealth.constants;

import org.springframework.http.MediaType;

public final class ApiConstants {

  public static final String MARKET_PRICE_BASE_URL = "/internal/prices";
  public static final String LATEST_PRICES_URL = MARKET_PRICE_BASE_URL + "/latest";
  public static final String PRICE_HISTORY_URL = MARKET_PRICE_BASE_URL + "/history";
  public static final String FROM_PARAM = "from";
  public static final String TO_PARAM = "to";
  public static final String REQUEST_PRODUCES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

  private ApiConstants() {}
}
