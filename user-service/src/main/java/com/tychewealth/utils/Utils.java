package com.tychewealth.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(key);
      byte[] hash = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HmacSHA256 algorithm not available", ex);
    }
  }

  public static String currentUtcDate(Clock clock) {
    return LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString();
  }

  public static Duration durationUntilNextUtcMidnight(Clock clock) {
    LocalDate tomorrow = LocalDate.now(clock.withZone(ZoneOffset.UTC)).plusDays(1);
    return Duration.between(clock.instant(), tomorrow.atStartOfDay().toInstant(ZoneOffset.UTC));
  }
}
