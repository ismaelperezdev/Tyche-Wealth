package com.tychewealth.error.exception;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EventPublishingException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> metadata;
  private final HttpStatus httpStatus;

  public EventPublishingException(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    this(resolve(errorDefinition), metadata, httpStatus);
  }

  public static EventPublishingException of(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    return new EventPublishingException(errorDefinition, metadata, httpStatus);
  }

  private EventPublishingException(
      ResolvedError resolvedError, Map<String, String> metadata, HttpStatus httpStatus) {
    super(resolvedError.errorDefinition().getDescription());

    this.errorDefinition = resolvedError.errorDefinition();
    this.metadata = sanitizeMetadata(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus;
  }

  private static ResolvedError resolve(ErrorDefinition errorDefinition) {
    return new ResolvedError(
        errorDefinition == null ? ErrorDefinition.GENERIC_INTERNAL_ERROR : errorDefinition);
  }

  private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }

    Map<String, String> sanitizedMetadata = new HashMap<>();
    metadata.forEach(
        (key, value) -> {
          if (key != null && value != null) {
            sanitizedMetadata.put(key, value);
          }
        });
    return sanitizedMetadata.isEmpty() ? Map.of() : Map.copyOf(sanitizedMetadata);
  }

  private record ResolvedError(ErrorDefinition errorDefinition) {}
}
