package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.RECEIVED;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_FILE_PART_NAME;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class AssetValidationHelperTest {

  private final AssetValidationHelper helper = new AssetValidationHelper(10L, 2, 5, 1, 1, 2);

  @Test
  void validateRetrievedImportExistsDoesNothingWhenPersistedImportExists() {
    AssetPersistRedisDto persistedImport =
        new AssetPersistRedisDto(
            TEST_ASSET_IMPORT_ID,
            1L,
            TEST_ASSET_FILE_NAME,
            Instant.now(),
            new AssetImportResponseDto());

    helper.validateRetrievedImportExists(persistedImport);
  }

  @Test
  void validateRetrievedImportExistsThrowsNotFoundWhenPersistedImportIsNull() {
    PortfolioException exception =
        assertThrows(PortfolioException.class, () -> helper.validateRetrievedImportExists(null));

    assertEquals(ErrorDefinition.ASSET_IMPORT_NOT_FOUND, exception.getErrorDefinition());
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void validateExtractionRequestRejectsNullFileNameAsGenericBadRequest() {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
    PortfolioException exception =
        assertThrows(
            PortfolioException.class, () -> helper.validateExtractionRequest(null, inputStream));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(
        PortfolioException.of(
                ErrorDefinition.GENERIC_BAD_REQUEST,
                Map.of(ERROR, "fileName must not be null"),
                HttpStatus.BAD_REQUEST)
            .getMessage(),
        exception.getMessage());
  }

  @Test
  void validateExtractionRequestRejectsNullInputStreamAsGenericBadRequest() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateExtractionRequest("statement.pdf", null));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(
        PortfolioException.of(
                ErrorDefinition.GENERIC_BAD_REQUEST,
                Map.of(ERROR, "inputStream must not be null"),
                HttpStatus.BAD_REQUEST)
            .getMessage(),
        exception.getMessage());
  }

  @Test
  void validateImportRequestRejectsFileLargerThanConfiguredLimit() {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME,
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            "12345678901".getBytes(StandardCharsets.UTF_8));

    AssetImportException exception =
        assertThrows(AssetImportException.class, () -> helper.validateImportRequest(1L, file));

    assertEquals(ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        AssetImportException.of(
                ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED,
                Map.of(EXPECTED, "10", RECEIVED, "11"),
                HttpStatus.BAD_REQUEST)
            .getMessage(),
        exception.getMessage());
  }

  @Test
  void validateImportRequestRejectsPdfWithTooManyPages() throws IOException {
    byte[] pdfBytes;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      document.addPage(new PDPage());
      document.addPage(new PDPage());
      document.save(outputStream);
      pdfBytes = outputStream.toByteArray();
    }

    AssetValidationHelper pageLimitedHelper =
        new AssetValidationHelper(pdfBytes.length + 100L, 2, 5, 1, 1, 2);
    MockMultipartFile file =
        new MockMultipartFile(TEST_FILE_PART_NAME, "statement.pdf", "application/pdf", pdfBytes);

    AssetImportException exception =
        assertThrows(
            AssetImportException.class, () -> pageLimitedHelper.validateImportRequest(1L, file));

    assertEquals(ErrorDefinition.ATTACHMENT_PAGE_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        AssetImportException.of(
                ErrorDefinition.ATTACHMENT_PAGE_LIMIT_EXCEEDED,
                Map.of(EXPECTED, "2", RECEIVED, "3"),
                HttpStatus.BAD_REQUEST)
            .getMessage(),
        exception.getMessage());
  }

  @Test
  void validateExtractedTextRejectsTooManyCharacters() {
    AssetImportException exception =
        assertThrows(AssetImportException.class, () -> helper.validateExtractedText("123456"));

    assertEquals(ErrorDefinition.ATTACHMENT_TEXT_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        AssetImportException.of(
                ErrorDefinition.ATTACHMENT_TEXT_LIMIT_EXCEEDED,
                Map.of(EXPECTED, "5", RECEIVED, "6"),
                HttpStatus.BAD_REQUEST)
            .getMessage(),
        exception.getMessage());
  }
}
