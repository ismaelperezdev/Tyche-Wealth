package com.tychewealth.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AiUtilsTest {

  @Test
  void sanitizeAiResponseExtractsJsonArrayFromMarkdownFenceAndTrailingText() {
    String aiResponse =
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
        """;

    String sanitized = AiUtils.sanitizeAiResponse(aiResponse);

    assertEquals(
        """
        [
          {
            "name": "Apple Inc.",
            "notes": "Bought after \\"earnings\\"",
            "symbol": "AAPL"
          }
        ]
        """
            .trim(),
        sanitized);
  }

  @Test
  void sanitizeAiResponseReturnsSubstringFromFirstJsonTokenWhenClosingTokenIsMissing() {
    String aiResponse = "Result: [{\"name\":\"Apple Inc.\"}";

    String sanitized = AiUtils.sanitizeAiResponse(aiResponse);

    assertEquals("[{\"name\":\"Apple Inc.\"}", sanitized);
  }

  @Test
  void sanitizeAiResponseReturnsTrimmedSourceWhenJsonIsNotPresent() {
    assertEquals("no json here", AiUtils.sanitizeAiResponse("  no json here  "));
  }
}
