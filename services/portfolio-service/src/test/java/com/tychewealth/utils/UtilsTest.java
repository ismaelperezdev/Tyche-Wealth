package com.tychewealth.utils;

import java.util.Map;

public final class UtilsTest {

  private UtilsTest() {}

  public static void putIfNotNull(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }
}
