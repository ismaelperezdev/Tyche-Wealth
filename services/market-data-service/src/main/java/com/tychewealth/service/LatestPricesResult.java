package com.tychewealth.service;

import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import java.util.List;

/** Available latest quotes and the total number of requested symbol candidates. */
public record LatestPricesResult(List<LatestPriceResponseDto> content, long totalCandidates) {

  public LatestPricesResult {
    content = List.copyOf(content);
  }
}
