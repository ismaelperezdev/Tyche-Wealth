package com.tychewealth.utils;

import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.RECEIVED;

import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public final class FileDataExtractor {

  private static final String SUPPORTED_EXTENSIONS = ".pdf, .csv, .xls, .xlsx";
  private static final String PDF_EXTENSION = ".pdf";
  private static final String CSV_EXTENSION = ".csv";
  private static final String XLS_EXTENSION = ".xls";
  private static final String XLSX_EXTENSION = ".xlsx";

  private FileDataExtractor() {}

  public static String extractText(String fileName, InputStream inputStream) {
    String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
    String extension = extensionOf(normalizedFileName);
    if (normalizedFileName.endsWith(PDF_EXTENSION)) {
      return extractPdfText(inputStream, extension);
    }
    if (normalizedFileName.endsWith(CSV_EXTENSION)) {
      return extractCsvText(inputStream, extension);
    }
    if (normalizedFileName.endsWith(XLS_EXTENSION) || normalizedFileName.endsWith(XLSX_EXTENSION)) {
      return extractExcelText(inputStream, extension);
    }

    throw extractionException(SUPPORTED_EXTENSIONS, extension);
  }

  private static String extractPdfText(InputStream inputStream, String extension) {
    try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
      PDFTextStripper textStripper = new PDFTextStripper();
      return normalizeLineEndings(textStripper.getText(document).trim());
    } catch (IOException ex) {
      throw extractionException(PDF_EXTENSION, extension);
    }
  }

  private static String extractCsvText(InputStream inputStream, String extension) {
    try {
      return normalizeLineEndings(new String(inputStream.readAllBytes()).trim());
    } catch (IOException ex) {
      throw extractionException(CSV_EXTENSION, extension);
    }
  }

  private static String extractExcelText(InputStream inputStream, String extension) {
    try (Workbook workbook = WorkbookFactory.create(inputStream)) {
      DataFormatter dataFormatter = new DataFormatter();
      StringBuilder builder = new StringBuilder();

      for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        if (builder.isEmpty()) {
          builder.append('\n');
        }
        appendSheet(builder, sheet, dataFormatter);
      }

      return normalizeLineEndings(builder.toString().trim());
    } catch (IOException ex) {
      throw extractionException(".xls or .xlsx", extension);
    }
  }

  private static void appendSheet(StringBuilder builder, Sheet sheet, DataFormatter dataFormatter) {
    for (Row row : sheet) {
      appendRow(builder, row, dataFormatter);
    }
  }

  private static void appendRow(StringBuilder builder, Row row, DataFormatter dataFormatter) {
    short lastCellNum = row.getLastCellNum();
    if (lastCellNum < 0) {
      builder.append('\n');
      return;
    }

    for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
      if (cellIndex > 0) {
        builder.append('\t');
      }
      Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (cell != null) {
        builder.append(dataFormatter.formatCellValue(cell));
      }
    }
    builder.append('\n');
  }

  private static String normalizeLineEndings(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }

  private static AssetImportException extractionException(String expected, String received) {
    return new AssetImportException(
        ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED,
        Map.of(EXPECTED, expected, RECEIVED, received),
        org.springframework.http.HttpStatus.BAD_REQUEST);
  }

  private static String extensionOf(String fileName) {
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
      return "[none]";
    }
    return fileName.substring(lastDotIndex);
  }
}
