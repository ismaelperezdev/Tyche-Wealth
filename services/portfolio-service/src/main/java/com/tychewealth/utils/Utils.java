package com.tychewealth.utils;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.UNKNOWN_VALUE;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

  public static PortfolioException genericBadRequest(String errorMessage) {
    return new PortfolioException(
        ErrorDefinition.GENERIC_BAD_REQUEST, Map.of(ERROR, errorMessage), HttpStatus.BAD_REQUEST);
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
    return originalFilename == null || originalFilename.isBlank()
        ? UNKNOWN_VALUE
        : originalFilename;
  }
}
