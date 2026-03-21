package com.tychewealth.error.exception;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EmailException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> metadata;
  private final HttpStatus httpStatus;

  public EmailException(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    this(resolve(errorDefinition), metadata, httpStatus);
  }

  public static EmailException of(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    return new EmailException(errorDefinition, metadata, httpStatus);
  }

  private EmailException(
      ResolvedError resolvedError, Map<String, String> metadata, HttpStatus httpStatus) {
    super(resolvedError.errorDefinition().getDescription());

    this.errorDefinition = resolvedError.errorDefinition();
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus;
  }

  private static ResolvedError resolve(ErrorDefinition errorDefinition) {
    return new ResolvedError(
        errorDefinition == null ? ErrorDefinition.EMAIL_DELIVERY_FAILED : errorDefinition);
  }

  private record ResolvedError(ErrorDefinition errorDefinition) {}
}
