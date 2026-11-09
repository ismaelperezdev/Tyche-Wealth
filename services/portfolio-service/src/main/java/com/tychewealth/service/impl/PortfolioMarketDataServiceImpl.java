package com.tychewealth.service.impl;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.marketdataclient.MarketDataClient;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.PortfolioMarketDataService;
import com.tychewealth.service.helper.marketdata.PortfolioMarketDataHelper;
import com.tychewealth.service.helper.marketdata.PortfolioMarketDataValidationHelper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Orchestrates market-data requests for the authenticated user's active assets. */
@Service
@RequiredArgsConstructor
public class PortfolioMarketDataServiceImpl implements PortfolioMarketDataService {

  private final AssetRepository assetRepository;
  private final PortfolioRepository portfolioRepository;
  private final PortfolioMarketDataValidationHelper validationHelper;
  private final PortfolioMarketDataHelper portfolioMarketDataHelper;
  private final MarketDataClient marketDataClient;

  @Value("${app.market-data.max-results:5000}")
  private int maxResults;

  @Override
  public List<AssetMarketPriceDto> getLatestPrices(
      Long userId, List<Long> portfolioIds, List<Long> assetIds, String authorizationHeader) {
    validationHelper.validateRequest(userId, portfolioIds, assetIds, null, null);
    List<AssetEntity> assets = resolveAssets(userId, portfolioIds, assetIds);
    if (assets.isEmpty()) {
      return List.of();
    }
    List<String> symbols = portfolioMarketDataHelper.symbolsFor(assets);
    MarketDataPage<LatestMarketPrice> page =
        marketDataClient.getLatestPrices(symbols, 0, maxResults, accessToken(authorizationHeader));
    validationHelper.validateLatestPriceCount(page);
    return portfolioMarketDataHelper.mapLatestPrices(assets, page);
  }

  @Override
  public List<AssetPriceCheckpointDto> getPriceHistory(
      Long userId,
      List<Long> portfolioIds,
      List<Long> assetIds,
      Instant from,
      Instant to,
      String authorizationHeader) {
    validationHelper.validateRequest(userId, portfolioIds, assetIds, from, to);
    List<AssetEntity> assets = resolveAssets(userId, portfolioIds, assetIds);
    if (assets.isEmpty()) {
      return List.of();
    }
    Instant effectiveFrom = portfolioMarketDataHelper.effectiveHistoryFrom(assets, from);
    if (to != null && to.isBefore(effectiveFrom)) {
      return List.of();
    }
    List<String> symbols = portfolioMarketDataHelper.symbolsFor(assets);
    MarketDataPage<PriceCheckpoint> page =
        marketDataClient.getPriceHistory(
            symbols, effectiveFrom, to, 0, maxResults, accessToken(authorizationHeader));
    validationHelper.validateHistoryCount(page);
    return portfolioMarketDataHelper.mapPriceHistory(assets, from, page);
  }

  private List<AssetEntity> resolveAssets(
      Long userId, List<Long> portfolioIds, List<Long> assetIds) {
    if (portfolioIds != null) {
      List<Long> distinctPortfolioIds = distinctIds(portfolioIds);
      List<PortfolioEntity> portfolios =
          portfolioRepository.findActiveByIdsAndUserId(distinctPortfolioIds, userId);
      validationHelper.validatePortfoliosResolved(distinctPortfolioIds, portfolios);
      return assetRepository.findActiveByPortfolioIdsAndUserId(distinctPortfolioIds, userId);
    }

    List<Long> distinctAssetIds = distinctIds(assetIds);
    List<AssetEntity> assets =
        assetRepository.findActiveByIdsAndPortfolioUserId(distinctAssetIds, userId);
    validationHelper.validateAssetsResolved(distinctAssetIds, assets);
    return assets;
  }

  private static List<Long> distinctIds(List<Long> ids) {
    return List.copyOf(new LinkedHashSet<>(ids));
  }

  private static String accessToken(String authorizationHeader) {
    return authorizationHeader.replaceFirst("(?i)^Bearer\\s+", "");
  }
}
