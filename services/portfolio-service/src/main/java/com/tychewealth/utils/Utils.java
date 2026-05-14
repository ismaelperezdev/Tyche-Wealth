package com.tychewealth.utils;

import static com.tychewealth.constants.CommonConstants.COMMA;
import static com.tychewealth.constants.CommonConstants.COMMA_CHAR;
import static com.tychewealth.constants.CommonConstants.DOT;
import static com.tychewealth.constants.CommonConstants.DOT_CHAR;
import static com.tychewealth.constants.CommonConstants.EMPTY_VALUE;
import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.SPACE;
import static com.tychewealth.constants.CommonConstants.UNKNOWN_VALUE;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public final class Utils {

  private static final String CONTROL_CHARACTERS_REGEX = "[\\u0000-\\u001F\\u007F]";
  private static final String WHITESPACE_REGEX = "\\s+";

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

  public static PortfolioException genericBadRequest(String errorMessage) {
    return new PortfolioException(
        ErrorDefinition.GENERIC_BAD_REQUEST,
        Map.of(ERROR, Objects.toString(errorMessage, "")),
        HttpStatus.BAD_REQUEST);
  }

  public static AuthException unauthorized() {
    return new AuthException(ErrorDefinition.UNAUTHORIZED, null, HttpStatus.UNAUTHORIZED);
  }

  public static ResponseStatusException rateLimited(String message) {
    String resolvedMessage =
        message == null || message.isBlank()
            ? ErrorDefinition.RATE_LIMITED.getDescription()
            : message;
    return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, resolvedMessage);
  }

  public static <T> ResponseEntity<T> buildNoStoreBodyResponse(HttpStatus status, T body) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .body(body);
  }

  public static ResponseEntity<Void> buildNoStoreEmptyResponse(HttpStatus status) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .build();
  }

  public static String sha256Hex(String value) {
    return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
  }

  public static String sha256Hex(byte[] value) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] hash = messageDigest.digest(value);
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }

  public static String resolveFileName(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      return UNKNOWN_VALUE;
    }

    String sanitized =
        originalFilename
            .replace('\\', '/')
            .replaceAll("^.*/", "")
            .replaceAll(CONTROL_CHARACTERS_REGEX, "")
            .replaceAll(WHITESPACE_REGEX, " ")
            .trim();
    return sanitized.isEmpty() ? UNKNOWN_VALUE : sanitized;
  }

  public static BigDecimal parseLocalizedNumber(String rawNumber) {
    String normalized = trimToNull(rawNumber);
    if (normalized == null) {
      return null;
    }

    try {
      return new BigDecimal(normalizeLocalizedNumber(normalized));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String normalizeLocalizedNumber(String rawNumber) {
    String normalized = rawNumber.replace(SPACE, EMPTY_VALUE);
    boolean containsComma = normalized.contains(COMMA);
    boolean containsDot = normalized.contains(DOT);

    if (!containsComma) {
      return normalized;
    }
    if (!containsDot) {
      return normalized.replace(COMMA_CHAR, DOT_CHAR);
    }
    if (normalized.lastIndexOf(COMMA_CHAR) > normalized.lastIndexOf(DOT_CHAR)) {
      return normalized.replace(DOT, EMPTY_VALUE).replace(COMMA_CHAR, DOT_CHAR);
    }
    return normalized.replace(COMMA, EMPTY_VALUE);
  }

  public static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
