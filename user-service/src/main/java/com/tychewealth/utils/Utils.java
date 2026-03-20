package com.tychewealth.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class Utils {

  private Utils() {}

  public static String normalizeIdentity(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  public static String sha256Hex(String value) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }

  public static String sha256Hex(String value, String pepper) {
    return sha256Hex(value + pepper);
  }
}
