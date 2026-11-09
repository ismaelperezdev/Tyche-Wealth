package com.tychewealth.service.helper.marketdata;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.mapper.marketdata.PortfolioMarketDataMapper;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Adapts market-data client responses to the Portfolio API response DTOs. */
@Component
public class PortfolioMarketDataHelper {

  private final PortfolioMarketDataMapper portfolioMarketDataMapper;

  public PortfolioMarketDataHelper(PortfolioMarketDataMapper portfolioMarketDataMapper) {
    this.portfolioMarketDataMapper = portfolioMarketDataMapper;
  }

  public List<AssetMarketPriceDto> mapLatestPrices(
      List<AssetEntity> assets, MarketDataPage<LatestMarketPrice> page) {
    Map<String, LatestMarketPrice> latestPrices = new LinkedHashMap<>();
    for (LatestMarketPrice latestPrice : page.content()) {
      latestPrices.put(normalizeSymbol(latestPrice.symbol()), latestPrice);
    }
    return assets.stream()
        .map(
            asset ->
                portfolioMarketDataMapper.toLatestPriceDto(
                    asset, latestPrices.get(normalizeSymbol(asset.getSymbol()))))
        .toList();
  }

  public List<String> symbolsFor(List<AssetEntity> assets) {
    return assets.stream()
        .map(AssetEntity::getSymbol)
        .map(PortfolioMarketDataHelper::normalizeSymbol)
        .distinct()
        .toList();
  }

  public List<AssetPriceCheckpointDto> mapPriceHistory(
      List<AssetEntity> assets, Instant from, MarketDataPage<PriceCheckpoint> page) {
    if (assets.isEmpty()) {
      return List.of();
    }
    Instant effectiveFrom = effectiveHistoryFrom(assets, from);
    Map<String, List<AssetEntity>> assetsBySymbol = new LinkedHashMap<>();
    for (AssetEntity asset : assets) {
      assetsBySymbol
          .computeIfAbsent(normalizeSymbol(asset.getSymbol()), ignored -> new ArrayList<>())
          .add(asset);
    }

    List<AssetPriceCheckpointDto> result = new ArrayList<>();
    for (PriceCheckpoint checkpoint : page.content()) {
      addCheckpointForAssets(
          result,
          assetsBySymbol.getOrDefault(normalizeSymbol(checkpoint.symbol()), List.of()),
          checkpoint,
          effectiveFrom);
    }
    return List.copyOf(result);
  }

  public long countPriceHistoryResults(
      List<AssetEntity> assets, Instant from, MarketDataPage<PriceCheckpoint> page) {
    if (assets.isEmpty()) {
      return 0;
    }

    Map<String, List<AssetEntity>> assetsBySymbol = new LinkedHashMap<>();
    for (AssetEntity asset : assets) {
      assetsBySymbol
          .computeIfAbsent(normalizeSymbol(asset.getSymbol()), ignored -> new ArrayList<>())
          .add(asset);
    }

    long resultCount = 0;
    for (PriceCheckpoint checkpoint : page.content()) {
      for (AssetEntity asset :
          assetsBySymbol.getOrDefault(normalizeSymbol(checkpoint.symbol()), List.of())) {
        if (checkpointAppliesToAsset(asset, checkpoint, from)) {
          resultCount++;
        }
      }
    }
    return resultCount;
  }

  public Instant effectiveHistoryFrom(List<AssetEntity> assets, Instant requestedFrom) {
    Instant oldestAssetCreatedAt =
        assets.stream()
            .map(PortfolioMarketDataHelper::assetCreatedAt)
            .min(Instant::compareTo)
            .orElseThrow();
    return requestedFrom == null || requestedFrom.isBefore(oldestAssetCreatedAt)
        ? oldestAssetCreatedAt
        : requestedFrom;
  }

  private void addCheckpointForAssets(
      List<AssetPriceCheckpointDto> result,
      List<AssetEntity> assets,
      PriceCheckpoint checkpoint,
      Instant requestedFrom) {
    for (AssetEntity asset : assets) {
      if (checkpointAppliesToAsset(asset, checkpoint, requestedFrom)) {
        result.add(portfolioMarketDataMapper.toCheckpointDto(asset, checkpoint));
      }
    }
  }

  private static boolean checkpointAppliesToAsset(
      AssetEntity asset, PriceCheckpoint checkpoint, Instant requestedFrom) {
    Instant assetFrom = assetCreatedAt(asset);
    if (requestedFrom != null && requestedFrom.isAfter(assetFrom)) {
      assetFrom = requestedFrom;
    }
    return !checkpoint.capturedAt().isBefore(assetFrom);
  }

  private static Instant assetCreatedAt(AssetEntity asset) {
    return asset.getCreatedAt().toInstant(ZoneOffset.UTC);
  }

  public static String normalizeSymbol(String symbol) {
    return symbol.trim().toUpperCase(Locale.ROOT);
  }
}
