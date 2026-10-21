package com.tychewealth.listing.exception;

import com.tychewealth.listing.enums.ErrorDefinition;
import java.text.MessageFormat;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Typed failure for invalid listing criteria.
 *
 * <p>The error definition provides the stable API code and message, while the HTTP status lets a
 * consuming service's error handler preserve the intended response contract.
 */
@Getter
public class FilterException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> description;
  private final HttpStatus httpStatus;

  public FilterException(ErrorDefinition errorDefinition, HttpStatus httpStatus) {
    this(errorDefinition, (Map<String, String>) null, httpStatus);
  }

  public FilterException(
      ErrorDefinition errorDefinition, Object[] arguments, HttpStatus httpStatus) {
    super(MessageFormat.format(errorDefinition.getDescription(), arguments));
    this.errorDefinition = errorDefinition;
    this.description = Map.of();
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }

  public FilterException(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    super(errorDefinition.getDescription());
    this.errorDefinition = errorDefinition;
    this.description = description == null ? Map.of() : Map.copyOf(description);
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }
}
