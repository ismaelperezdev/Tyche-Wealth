package com.tychewealth.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;

public final class Utils {

  private Utils() {}

  public static boolean hasConstraintViolation(Throwable throwable, String constraintName) {
    Throwable current = throwable;

    while (current != null) {
      if (current instanceof ConstraintViolationException cve
          && containsConstraintName(cve.getConstraintName(), constraintName)) {
        return true;
      }

      if (containsConstraintName(current.getMessage(), constraintName)) {
        return true;
      }

      current = current.getCause();
    }

    return false;
  }

  public static boolean containsConstraintName(String source, String constraintName) {
    return source != null
        && constraintName != null
        && source.toLowerCase(Locale.ROOT).contains(constraintName.toLowerCase(Locale.ROOT));
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
}
