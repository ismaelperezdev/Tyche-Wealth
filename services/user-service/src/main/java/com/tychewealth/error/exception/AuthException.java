package com.tychewealth.error.exception;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> metadata;
  private final HttpStatus httpStatus;

  public AuthException(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    this(resolve(errorDefinition), metadata, httpStatus);
  }

  public static AuthException of(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    return new AuthException(errorDefinition, metadata, httpStatus);
  }

  private AuthException(
      ResolvedError resolvedError, Map<String, String> metadata, HttpStatus httpStatus) {
    super(resolvedError.errorDefinition().getDescription());

    this.errorDefinition = resolvedError.errorDefinition();
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }

  private static ResolvedError resolve(ErrorDefinition errorDefinition) {
    return new ResolvedError(errorDefinition == null ? ErrorDefinition.CONFLICT : errorDefinition);
  }

  private record ResolvedError(ErrorDefinition errorDefinition) {}
}
