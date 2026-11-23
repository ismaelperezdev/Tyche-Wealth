package com.tychewealth.testdata;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SecurityTestHelper {

  private SecurityTestHelper() {}

  public static String basicAuth() {
    String credentials = "prometheus-scraper:test-prometheus-password";
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
