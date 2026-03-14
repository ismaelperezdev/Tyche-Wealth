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

    super(resolveError(errorDefinition).getDescription());

    this.errorDefinition = resolveError(errorDefinition);
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }

  public static UserException of(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    return new UserException(errorDefinition, metadata, httpStatus);
  }

  private static ErrorDefinition resolveError(ErrorDefinition errorDefinition) {
    return errorDefinition == null ? ErrorDefinition.CONFLICT : errorDefinition;
  }
}
