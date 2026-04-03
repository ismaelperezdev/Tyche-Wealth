package com.tychewealth.error.exception;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> description;
  private final HttpStatus httpStatus;

  public AuthException(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    this(resolve(errorDefinition), description, httpStatus);
  }

  public static AuthException of(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    return new AuthException(errorDefinition, description, httpStatus);
  }

  private AuthException(
      ResolvedError resolvedError, Map<String, String> description, HttpStatus httpStatus) {
    super(resolvedError.errorDefinition().getDescription());

    this.errorDefinition = resolvedError.errorDefinition();
    this.description = description == null ? Map.of() : Map.copyOf(description);
    this.httpStatus = httpStatus == null ? HttpStatus.UNAUTHORIZED : httpStatus;
  }

  private static ResolvedError resolve(ErrorDefinition errorDefinition) {
    return new ResolvedError(
        errorDefinition == null ? ErrorDefinition.UNAUTHORIZED : errorDefinition);
  }

  private record ResolvedError(ErrorDefinition errorDefinition) {}
}
