package com.tychewealth.model;

import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import java.util.Map;

/** Latest quotes and checkpoint fallbacks needed for the final valuation point. */
public record PortfolioValuationEndPrices(
    Map<String, LatestMarketPrice> latestPrices, Map<String, PriceCheckpoint> checkpoints) {

  public PortfolioValuationEndPrices {
    latestPrices = Map.copyOf(latestPrices);
    checkpoints = Map.copyOf(checkpoints);
  }
}
