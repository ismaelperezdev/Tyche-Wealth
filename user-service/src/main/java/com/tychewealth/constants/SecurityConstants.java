package com.tychewealth.constants;

public final class SecurityConstants {

  public static final String ACTUATOR_PROMETHEUS_PATH = "/actuator/prometheus";
  public static final String CACHE_CONTROL_NO_STORE_HEADER_VALUE =
      "no-store, must-revalidate, private";
  public static final String PRAGMA_NO_CACHE_HEADER_VALUE = "no-cache";
  public static final String HEADER_VALUE_NOSNIFF = "nosniff";
  public static final String HEADER_VALUE_DENY = "DENY";
  public static final String HEADER_VALUE_NO_REFERRER = "no-referrer";
  public static final long HSTS_MAX_AGE_SECONDS = 31536000L;

  private SecurityConstants() {}
}
