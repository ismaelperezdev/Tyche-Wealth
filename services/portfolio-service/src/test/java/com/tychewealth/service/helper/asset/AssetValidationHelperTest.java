package com.tychewealth.service.helper.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class AssetValidationHelperTest {

  private final AssetValidationHelper helper = new AssetValidationHelper(10L, 2, 5, 1, 1, 2);

  @Test
  void validateExtractionRequestRejectsNullFileNameAsGenericBadRequest() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                helper.validateExtractionRequest(
                    null, new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals("The request is invalid: fileName must not be null", exception.getMessage());
  }

  @Test
  void validateExtractionRequestRejectsNullInputStreamAsGenericBadRequest() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateExtractionRequest("statement.pdf", null));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals("The request is invalid: inputStream must not be null", exception.getMessage());
  }

  @Test
  void validateImportRequestRejectsFileLargerThanConfiguredLimit() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "positions.csv", "text/csv", "12345678901".getBytes(StandardCharsets.UTF_8));

    AssetImportException exception =
        assertThrows(AssetImportException.class, () -> helper.validateImportRequest(1L, file));

    assertEquals(ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        "The attachment exceeds the maximum allowed size. Maximum: 10 bytes. Received: 11 bytes",
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
        new MockMultipartFile("file", "statement.pdf", "application/pdf", pdfBytes);

    AssetImportException exception =
        assertThrows(
            AssetImportException.class, () -> pageLimitedHelper.validateImportRequest(1L, file));

    assertEquals(ErrorDefinition.ATTACHMENT_PAGE_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        "The attachment exceeds the maximum allowed page count. Maximum: 2. Received: 3",
        exception.getMessage());
  }

  @Test
  void validateExtractedTextRejectsTooManyCharacters() {
    AssetImportException exception =
        assertThrows(AssetImportException.class, () -> helper.validateExtractedText("123456"));

    assertEquals(ErrorDefinition.ATTACHMENT_TEXT_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        "The extracted attachment text exceeds the maximum allowed length. Maximum: 5 characters. Received: 6 characters",
        exception.getMessage());
  }
}
