package com.tychewealth.marketdataclient.dto;

import java.util.List;

/** A page of market-data results and the total number of matching records. */
public record MarketDataPage<T>(List<T> content, long totalCount) {

  public MarketDataPage {
    content = content == null ? List.of() : List.copyOf(content);
  }
}
