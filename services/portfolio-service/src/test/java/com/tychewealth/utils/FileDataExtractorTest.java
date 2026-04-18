package com.tychewealth.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class FileDataExtractorTest {

  @Test
  void extractTextReturnsCsvContent() {
    String result =
        FileDataExtractor.extractText(
            "positions.csv",
            new ByteArrayInputStream(
                "symbol,quantity\nAAPL,10\nMSFT,5".getBytes(StandardCharsets.UTF_8)));

    assertEquals("symbol,quantity\nAAPL,10\nMSFT,5", result);
  }

  @Test
  void extractTextReturnsExcelContent() throws IOException {
    byte[] workbookBytes;
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Assets");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("symbol");
      header.createCell(1).setCellValue("quantity");
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("AAPL");
      row.createCell(1).setCellValue(10);
      workbook.write(outputStream);
      workbookBytes = outputStream.toByteArray();
    }

    String result =
        FileDataExtractor.extractText("positions.xlsx", new ByteArrayInputStream(workbookBytes));

    assertEquals("symbol\tquantity\nAAPL\t10", result);
  }

  @Test
  void extractTextReturnsPdfContent() throws IOException {
    byte[] pdfBytes;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contentStream.newLineAtOffset(50, 700);
        contentStream.showText("AAPL 10 USD");
        contentStream.endText();
      }
      document.save(outputStream);
      pdfBytes = outputStream.toByteArray();
    }

    String result =
        FileDataExtractor.extractText("statement.pdf", new ByteArrayInputStream(pdfBytes));

    assertEquals("AAPL 10 USD", result);
  }

  @Test
  void extractTextRejectsUnsupportedFileType() {
    AssetImportException exception =
        assertThrows(
            AssetImportException.class,
            () ->
                FileDataExtractor.extractText(
                    "statement.txt",
                    new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))));

    assertEquals(ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED, exception.getErrorDefinition());
    assertEquals(
        "Unable to extract text from file. Expected: .pdf, .csv, .xls, .xlsx. Received: .txt",
        exception.getMessage());
  }
}
