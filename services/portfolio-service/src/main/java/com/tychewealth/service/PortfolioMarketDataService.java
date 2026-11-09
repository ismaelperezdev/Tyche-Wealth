package com.tychewealth.service;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import java.time.Instant;
import java.util.List;

/** Application service for market data associated with the authenticated user's assets. */
public interface PortfolioMarketDataService {

  List<AssetMarketPriceDto> getLatestPrices(
      Long userId, List<Long> portfolioIds, List<Long> assetIds, String authorizationHeader);

  List<AssetPriceCheckpointDto> getPriceHistory(
      Long userId,
      List<Long> portfolioIds,
      List<Long> assetIds,
      Instant from,
      Instant to,
      String authorizationHeader);
}
