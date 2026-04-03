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
  private final Map<String, String> description;
  private final HttpStatus httpStatus;

  public PortfolioException(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    this(resolve(errorDefinition, description), description, httpStatus);
  }

  public static PortfolioException of(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    return new PortfolioException(errorDefinition, description, httpStatus);
  }

  private PortfolioException(
      ResolvedError resolvedError, Map<String, String> description, HttpStatus httpStatus) {
    super(resolvedError.description());

    this.errorDefinition = resolvedError.errorDefinition();
    this.description = description == null ? Map.of() : Map.copyOf(description);
    this.httpStatus = httpStatus == null ? HttpStatus.CONFLICT : httpStatus;
  }

  private static ResolvedError resolve(
      ErrorDefinition errorDefinition, Map<String, String> description) {
    ErrorDefinition resolvedErrorDefinition =
        errorDefinition == null ? ErrorDefinition.CONFLICT : errorDefinition;
    return new ResolvedError(
        resolvedErrorDefinition, resolveDescription(resolvedErrorDefinition, description));
  }

  private static String resolveDescription(
      ErrorDefinition errorDefinition, Map<String, String> description) {
    String resolvedDescription = errorDefinition.getDescription();
    String name = description == null || description.get(NAME) == null ? "" : description.get(NAME);
    String error =
        description == null || description.get(ERROR) == null ? "" : description.get(ERROR);
    return resolvedDescription.replace(NAME_PLACEHOLDER, name).replace(ERROR_PLACEHOLDER, error);
  }

  private record ResolvedError(ErrorDefinition errorDefinition, String description) {}
}
