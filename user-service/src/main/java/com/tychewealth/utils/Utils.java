package com.tychewealth.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

  public static String hmacSha256Hex(String value, String pepper) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(key);
      byte[] hash = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to compute HmacSHA256 hash", ex);
    }
  }

  public static String currentUtcDate(Clock clock) {
    return LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString();
  }

  public static Duration durationUntilNextUtcMidnight(Clock clock) {
    Instant now = clock.instant();
    LocalDate tomorrow = now.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1);
    return Duration.between(now, tomorrow.atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  public static String formatExpirationText(long expiresInSeconds) {
    if (expiresInSeconds % 3600 == 0) {
      long hours = expiresInSeconds / 3600;
      return hours + (hours == 1 ? " hour" : " hours");
    }

    long expiresInMinutes = (expiresInSeconds + 59) / 60;
    return expiresInMinutes + (expiresInMinutes == 1 ? " minute" : " minutes");
  }
}
