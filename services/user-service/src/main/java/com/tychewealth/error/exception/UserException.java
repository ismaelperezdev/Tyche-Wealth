package com.tychewealth.error.exception;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> metadata;
  private final HttpStatus httpStatus;

  public UserException(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    this(resolve(errorDefinition), metadata, httpStatus);
  }

  private UserException(
      ResolvedError resolvedError, Map<String, String> metadata, HttpStatus httpStatus) {
    super(resolvedError.errorDefinition().getDescription());

    this.errorDefinition = resolvedError.errorDefinition();
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }

  public static UserException of(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    return new UserException(errorDefinition, metadata, httpStatus);
  }

  private static ResolvedError resolve(ErrorDefinition errorDefinition) {
    return new ResolvedError(errorDefinition == null ? ErrorDefinition.CONFLICT : errorDefinition);
  }

  private record ResolvedError(ErrorDefinition errorDefinition) {}
}
