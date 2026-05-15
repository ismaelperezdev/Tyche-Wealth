package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.RECEIVED;
import static com.tychewealth.constants.LogConstants.MISSING_AUTHENTICATED_USER_MESSAGE;

import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.utils.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AssetValidationHelper {

  private static final String MISSING_FILE_MESSAGE = "file must not be null";
  private static final String EMPTY_FILE_MESSAGE = "file must not be empty";
  private static final String FILE_NAME_REQUIRED_MESSAGE = "fileName must not be null";
  private static final String INPUT_STREAM_REQUIRED_MESSAGE = "inputStream must not be null";
  private final long maxFileSizeBytes;
  private final int maxPdfPages;
  private final int maxExtractedCharacters;
  private final int maxExtractionProcessingSeconds;
  private final int maxAiProcessingSeconds;
  private final int maxDetectedAssets;

  public AssetValidationHelper(
      @Value("${app.asset.import.validation.max-file-size-bytes:3145728}") long maxFileSizeBytes,
      @Value("${app.asset.import.validation.max-pdf-pages:10}") int maxPdfPages,
      @Value("${app.asset.import.validation.max-extracted-characters:15000}")
          int maxExtractedCharacters,
      @Value("${app.asset.import.validation.max-extraction-processing-seconds:15}")
          int maxExtractionProcessingSeconds,
      @Value("${app.asset.import.validation.max-ai-processing-seconds:90}")
          int maxAiProcessingSeconds,
      @Value("${app.asset.import.validation.max-detected-assets:25}") int maxDetectedAssets) {
    this.maxFileSizeBytes = maxFileSizeBytes <= 0 ? 3145728L : maxFileSizeBytes;
    this.maxPdfPages = maxPdfPages <= 0 ? 10 : maxPdfPages;
    this.maxExtractedCharacters = maxExtractedCharacters <= 0 ? 15000 : maxExtractedCharacters;
    this.maxExtractionProcessingSeconds =
        maxExtractionProcessingSeconds <= 0 ? 15 : maxExtractionProcessingSeconds;
    this.maxAiProcessingSeconds = maxAiProcessingSeconds <= 0 ? 90 : maxAiProcessingSeconds;
    this.maxDetectedAssets = maxDetectedAssets <= 0 ? 25 : maxDetectedAssets;
  }

  public void validateImportRequest(Long userId, MultipartFile file) {
    validateAuthenticatedUser(userId);
    validateFile(file);
    validateFileSize(file);
    validatePdfPageCount(file);
  }

  public void validateExtractionRequest(String fileName, InputStream inputStream) {
    validateFileName(fileName);
    validateInputStream(inputStream);
  }

  public void validateAuthenticatedUser(Long userId) {
    if (userId == null) {
      throw genericBadRequest(MISSING_AUTHENTICATED_USER_MESSAGE);
    }
  }

  public void validateRetrievedImportExists(AssetPersistRedisDto persistedImport) {
    if (persistedImport == null) {
      throw new PortfolioException(
          ErrorDefinition.ASSET_IMPORT_NOT_FOUND, Map.of(), HttpStatus.NOT_FOUND);
    }
  }

  public void validateFile(MultipartFile file) {
    if (file == null) {
      throw genericBadRequest(MISSING_FILE_MESSAGE);
    }
    if (file.isEmpty()) {
      throw genericBadRequest(EMPTY_FILE_MESSAGE);
    }
  }

  public void validateFileName(String fileName) {
    if (fileName == null) {
      throw genericBadRequest(FILE_NAME_REQUIRED_MESSAGE);
    }
  }

  public void validateInputStream(InputStream inputStream) {
    if (inputStream == null) {
      throw genericBadRequest(INPUT_STREAM_REQUIRED_MESSAGE);
    }
  }

  public void validateExtractedText(String extractedText) {
    if (extractedText != null && extractedText.length() > maxExtractedCharacters) {
      throw new AssetImportException(
          ErrorDefinition.ATTACHMENT_TEXT_LIMIT_EXCEEDED,
          Map.of(
              EXPECTED,
              String.valueOf(maxExtractedCharacters),
              RECEIVED,
              String.valueOf(extractedText.length())),
          HttpStatus.BAD_REQUEST);
    }
  }

  public void validateFileSize(MultipartFile file) {
    if (file.getSize() > maxFileSizeBytes) {
      throw new AssetImportException(
          ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED,
          Map.of(
              EXPECTED, String.valueOf(maxFileSizeBytes), RECEIVED, String.valueOf(file.getSize())),
          HttpStatus.BAD_REQUEST);
    }
  }

  public void validatePdfPageCount(MultipartFile file) {
    String fileName = Utils.resolveFileName(file);
    if (!fileName.toLowerCase().endsWith(".pdf")) {
      return;
    }

    try (PDDocument document = Loader.loadPDF(file.getBytes())) {
      if (document.getNumberOfPages() > maxPdfPages) {
        throw new AssetImportException(
            ErrorDefinition.ATTACHMENT_PAGE_LIMIT_EXCEEDED,
            Map.of(
                EXPECTED,
                String.valueOf(maxPdfPages),
                RECEIVED,
                String.valueOf(document.getNumberOfPages())),
            HttpStatus.BAD_REQUEST);
      }
    } catch (AssetImportException ex) {
      throw ex;
    } catch (IOException | RuntimeException ex) {
      throw new AssetImportException(
          ErrorDefinition.ATTACHMENT_INSPECTION_FAILED,
          Map.of(
              ERROR,
              ex.getMessage() == null ? "invalid or unsupported PDF document" : ex.getMessage()),
          HttpStatus.BAD_REQUEST);
    }
  }

  public int extractionTimeoutSeconds() {
    return maxExtractionProcessingSeconds;
  }

  public int aiTimeoutSeconds() {
    return maxAiProcessingSeconds;
  }

  public void validateDetectedAssetsCount(int detectedAssets) {
    if (detectedAssets > maxDetectedAssets) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_RESULT_LIMIT_EXCEEDED,
          Map.of(
              EXPECTED,
              String.valueOf(maxDetectedAssets),
              RECEIVED,
              String.valueOf(detectedAssets)),
          HttpStatus.BAD_REQUEST);
    }
  }

  public AssetImportException extractionTimeoutExceeded(long elapsedSeconds) {
    return new AssetImportException(
        ErrorDefinition.ATTACHMENT_PROCESSING_TIMEOUT_EXCEEDED,
        Map.of(
            EXPECTED,
            String.valueOf(maxExtractionProcessingSeconds),
            RECEIVED,
            String.valueOf(elapsedSeconds)),
        HttpStatus.BAD_REQUEST);
  }

  public AssetImportException aiTimeoutExceeded(long elapsedSeconds) {
    return new AssetImportException(
        ErrorDefinition.AI_PROCESSING_TIMEOUT_EXCEEDED,
        Map.of(
            EXPECTED,
            String.valueOf(maxAiProcessingSeconds),
            RECEIVED,
            String.valueOf(elapsedSeconds)),
        HttpStatus.BAD_REQUEST);
  }

  private PortfolioException genericBadRequest(String errorMessage) {
    return new PortfolioException(
        ErrorDefinition.GENERIC_BAD_REQUEST, Map.of(ERROR, errorMessage), HttpStatus.BAD_REQUEST);
  }
}
