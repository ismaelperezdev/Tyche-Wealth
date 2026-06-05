package com.tychewealth.testdata;

public final class TwelveDataClientTestData {

  private TwelveDataClientTestData() {}

  public static final String QUOTE_RESPONSE =
      """
      {
        "symbol": "AAPL",
        "name": "Apple Inc",
        "exchange": "NASDAQ",
        "currency": "USD",
        "close": "187.24",
        "percent_change": "0.67"
      }
      """;

  public static final String PRICE_RESPONSE =
      """
      {
        "price": "187.24"
      }
      """;

  public static final String TIME_SERIES_RESPONSE =
      """
      {
        "meta": {
          "symbol": "AAPL",
          "interval": "4min"
        },
        "values": [
          {
            "datetime": "2026-06-05 16:40:00",
            "close": "187.24"
          }
        ]
      }
      """;
}
