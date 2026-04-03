package com.tychewealth.error.exception;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.ERROR_PLACEHOLDER;
import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.EXPECTED_PLACEHOLDER;
import static com.tychewealth.constants.CommonConstants.RECEIVED;
import static com.tychewealth.constants.CommonConstants.RECEIVED_PLACEHOLDER;

import com.tychewealth.error.handler.ErrorDefinition;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AssetImportException extends RuntimeException {

  private final ErrorDefinition errorDefinition;
  private final Map<String, String> description;
  private final HttpStatus httpStatus;

  public AssetImportException(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    this(resolve(errorDefinition, description), description, httpStatus);
  }

  public static AssetImportException of(
      ErrorDefinition errorDefinition, Map<String, String> description, HttpStatus httpStatus) {
    return new AssetImportException(errorDefinition, description, httpStatus);
  }

  private AssetImportException(
      ResolvedError resolvedError, Map<String, String> description, HttpStatus httpStatus) {
    super(resolvedError.description());

    this.errorDefinition = resolvedError.errorDefinition();
    this.description = description == null ? Map.of() : Map.copyOf(description);
    this.httpStatus = httpStatus == null ? HttpStatus.BAD_REQUEST : httpStatus;
  }

  private static ResolvedError resolve(
      ErrorDefinition errorDefinition, Map<String, String> description) {
    ErrorDefinition resolvedErrorDefinition =
        errorDefinition == null ? ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED : errorDefinition;
    return new ResolvedError(
        resolvedErrorDefinition, resolveDescription(resolvedErrorDefinition, description));
  }

  private static String resolveDescription(
      ErrorDefinition errorDefinition, Map<String, String> description) {
    String resolvedDescription = errorDefinition.getDescription();
    String error =
        description == null || description.get(ERROR) == null ? "" : description.get(ERROR);
    String expected =
        description == null || description.get(EXPECTED) == null ? "" : description.get(EXPECTED);
    String received =
        description == null || description.get(RECEIVED) == null ? "" : description.get(RECEIVED);
    return resolvedDescription
        .replace(ERROR_PLACEHOLDER, error)
        .replace(EXPECTED_PLACEHOLDER, expected)
        .replace(RECEIVED_PLACEHOLDER, received);
  }

  private record ResolvedError(ErrorDefinition errorDefinition, String description) {}
}
