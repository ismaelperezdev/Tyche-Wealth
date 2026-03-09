package com.tychewealth.utils;

import java.util.Locale;

public final class Utils {

  private Utils() {}

  public static String normalizeIdentity(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}
