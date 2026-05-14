package com.tychewealth.testdata;

public final class AiTestData {

  public static final long TEST_MAX_FILE_SIZE_BYTES = 1024L;
  public static final int TEST_MAX_PDF_PAGES = 10;
  public static final int TEST_MAX_EXTRACTED_CHARACTERS = 5000;
  public static final int TEST_EXTRACTION_TIMEOUT_SECONDS = 5;
  public static final int TEST_AI_TIMEOUT_SECONDS = 3;
  public static final int TEST_AI_TIMEOUT_SECONDS_TIGHT = 1;
  public static final int TEST_MAX_DETECTED_ASSETS = 10;
  public static final int TEST_MAX_DETECTED_ASSETS_TIGHT = 1;

  public static final int TEST_EXECUTOR_CONCURRENCY = 1;
  public static final int TEST_EXECUTOR_QUEUE_CAPACITY = 1;
  public static final long TEST_QUEUE_OFFER_TIMEOUT_SECONDS = 1L;
  public static final long TEST_AWAIT_TIMEOUT_SECONDS = 1L;
  public static final long TEST_EXPECTED_TIMEOUT_UPPER_BOUND_MILLIS = 1500L;

  public static final String TEST_PROMPT_1 = "p1";
  public static final String TEST_PROMPT_2 = "p2";
  public static final String TEST_PROMPT_3 = "p3";
  public static final String TEST_EMPTY_AI_RESPONSE = "[]";
  public static final String TEST_AI_QUEUE_FULL_REASON = "AI queue is full";
  public static final String TEST_BLANK_VALUE = " ";
  public static final String TEST_ASSET_ISIN_APPLE = "US0378331005";
  public static final String TEST_AI_RESPONSE_MARKDOWN_WITH_TRAILING_TEXT =
      """
      ```json
      [
        {
          "name":"Apple Inc.",
          "symbol":"AAPL",
          "assetType":"STOCK",
          "quantity":10,
          "averagePrice":150.00,
          "currency":"USD"
        }
      ]
      ```
      trailing text
      """;
  public static final String TEST_AI_RESPONSE_WITH_NULL_AND_EMPTY_CANDIDATES =
      """
      [
        null,
        {},
        {
          "name":"Apple Inc.",
          "symbol":"AAPL",
          "assetType":"STOCK"
        }
      ]
      """;
  public static final String TEST_AI_RESPONSE_TWO_STOCKS =
      """
      [
        {
          "name":"Apple Inc.",
          "symbol":"AAPL",
          "assetType":"STOCK"
        },
        {
          "name":"Microsoft Corp.",
          "symbol":"MSFT",
          "assetType":"STOCK"
        }
      ]
      """;
  public static final String TEST_AI_RESPONSE_INVALID_JSON = "[{\"symbol\":\"AAPL\",]";
  public static final String TEST_EXTRACTED_STATEMENT_SINGLE_HOLDING =
      """
      Account currency USD
      10 ud Apple Inc.
      US0378331005
      150.00 USD
      number of positions
      """;
  public static final String TEST_ASSET_NAME_MICROSOFT = "Microsoft Corp.";
  public static final String TEST_ASSET_SYMBOL_MSFT = "MSFT";

  private AiTestData() {}
}
