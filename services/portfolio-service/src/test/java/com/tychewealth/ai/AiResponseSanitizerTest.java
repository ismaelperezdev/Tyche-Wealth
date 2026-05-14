package com.tychewealth.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AiResponseSanitizerTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("sanitizeAiResponseCases")
  void sanitizeAiResponseReturnsExpectedContent(
      String testName, String aiResponse, String expectedSanitizedResponse) {
    assertEquals(expectedSanitizedResponse, AiResponseSanitizer.sanitizeAiResponse(aiResponse));
  }

  private static Stream<Arguments> sanitizeAiResponseCases() {
    return Stream.of(
        Arguments.of(
            "extracts json array from markdown fence and trailing text",
            """
            ```json
            [
              {
                "name": "Apple Inc.",
                "notes": "Bought after \\"earnings\\"",
                "symbol": "AAPL"
              }
            ]
            ```
            extra trailing text
            """,
            """
            [
              {
                "name": "Apple Inc.",
                "notes": "Bought after \\"earnings\\"",
                "symbol": "AAPL"
              }
            ]
            """
                .trim()),
        Arguments.of(
            "returns substring from first json token when closing token is missing",
            "Result: [{\"name\":\"Apple Inc.\"}",
            "[{\"name\":\"Apple Inc.\"}"),
        Arguments.of(
            "ignores delimiters inside escaped quotes",
            """
            {
              "notes": "quoted brace: \\"}\\"",
              "symbol": "AAPL"
            }
            trailing text
            """,
            """
            {
              "notes": "quoted brace: \\"}\\"",
              "symbol": "AAPL"
            }
            """
                .trim()),
        Arguments.of(
            "handles escaped quotes with brackets and braces",
            """
            AI output:
            {
              "note": "contains \\" ] } inside\\"",
              "symbol": "AAPL"
            }
            trailing text
            """,
            """
            {
              "note": "contains \\" ] } inside\\"",
              "symbol": "AAPL"
            }
            """
                .trim()),
        Arguments.of(
            "strips line comments outside strings",
            """
            [
              {
                "name": "Apple Inc.", // equity name
                "symbol": "AAPL",
                "notes": "keep // inside string"
              }
            ]
            """,
            """
            [
              {
                "name": "Apple Inc.",
                "symbol": "AAPL",
                "notes": "keep // inside string"
              }
            ]
            """
                .trim()),
        Arguments.of(
            "returns trimmed source when json is not present", "  no json here  ", "no json here"),
        Arguments.of(
            "quotes bare string values after colon",
            """
            [
              {
                "symbol": IE00B4ND3602,
                "assetType": STOCK
              }
            ]
            """,
            """
            [
              {
                "symbol": "IE00B4ND3602",
                "assetType": "STOCK"
              }
            ]
            """
                .trim()));
  }
}
