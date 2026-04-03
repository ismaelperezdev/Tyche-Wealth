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
  private final Map<String, String> metadata;
  private final HttpStatus httpStatus;

  public AssetImportException(
      ErrorDefinition errorDefinition, Map<String, String> metadata, HttpStatus httpStatus) {
    super(resolveDescription(errorDefinition, metadata));
    this.errorDefinition = resolve(errorDefinition);
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.httpStatus = httpStatus == null ? HttpStatus.BAD_REQUEST : httpStatus;
  }

  private static ErrorDefinition resolve(ErrorDefinition errorDefinition) {
    return errorDefinition == null
        ? ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED
        : errorDefinition;
  }

  private static String resolveDescription(
      ErrorDefinition errorDefinition, Map<String, String> metadata) {
    String description = resolve(errorDefinition).getDescription();
    String error = metadata == null || metadata.get(ERROR) == null ? "" : metadata.get(ERROR);
    String expected =
        metadata == null || metadata.get(EXPECTED) == null ? "" : metadata.get(EXPECTED);
    String received =
        metadata == null || metadata.get(RECEIVED) == null ? "" : metadata.get(RECEIVED);
    return description
        .replace(ERROR_PLACEHOLDER, error)
        .replace(EXPECTED_PLACEHOLDER, expected)
        .replace(RECEIVED_PLACEHOLDER, received);
  }
}
