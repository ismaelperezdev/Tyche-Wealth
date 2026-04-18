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
    super(resolve(errorDefinition).getDescription());

    this.errorDefinition = resolve(errorDefinition);
    this.description = description == null ? Map.of() : Map.copyOf(description);
    this.httpStatus = httpStatus == null ? HttpStatus.UNAUTHORIZED : httpStatus;
  }

  public static AuthException of(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    return new AuthException(errorDefinition, description, httpStatus);
  }

  private static ErrorDefinition resolve(ErrorDefinition errorDefinition) {
    return errorDefinition == null ? ErrorDefinition.UNAUTHORIZED : errorDefinition;
  }
}
