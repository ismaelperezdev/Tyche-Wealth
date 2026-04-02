package com.tychewealth.error.exception;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.ERROR_PLACEHOLDER;
import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.CommonConstants.NAME_PLACEHOLDER;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PortfolioException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> metadata;
  private final HttpStatus httpStatus;

  public PortfolioException(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    super(resolveDescription(errorDefinition, metadata));
    this.errorDefinition = resolve(errorDefinition);
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }

  private static ErrorDefinition resolve(ErrorDefinition errorDefinition) {
    return errorDefinition == null ? ErrorDefinition.CONFLICT : errorDefinition;
  }

  private static String resolveDescription(
      ErrorDefinition errorDefinition, Map<String, String> metadata) {
    String description = resolve(errorDefinition).getDescription();
    String name = metadata == null || metadata.get(NAME) == null ? "" : metadata.get(NAME);
    String error = metadata == null || metadata.get(ERROR) == null ? "" : metadata.get(ERROR);
    return description.replace(NAME_PLACEHOLDER, name).replace(ERROR_PLACEHOLDER, error);
  }
}
