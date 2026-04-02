package com.tychewealth.utils;

public final class LogContextFactory {

  private LogContextFactory() {}

  public static String action(String action) {
    return "[" + action + "]";
  }

  public static String mask(String value) {
    if (value == null || value.isBlank()) {
      return "<empty>";
    }
    if (value.length() <= 2) {
      return "**";
    }
    return value.substring(0, 2) + "***";
  }
}
