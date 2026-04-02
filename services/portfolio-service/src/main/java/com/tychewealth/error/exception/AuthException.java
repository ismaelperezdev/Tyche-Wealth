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
    super(resolve(errorDefinition).getDescription());
    this.errorDefinition = resolve(errorDefinition);
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.UNAUTHORIZED : httpStatus;
  }

  private static ErrorDefinition resolve(ErrorDefinition errorDefinition) {
    return errorDefinition == null ? ErrorDefinition.UNAUTHORIZED : errorDefinition;
  }
}
