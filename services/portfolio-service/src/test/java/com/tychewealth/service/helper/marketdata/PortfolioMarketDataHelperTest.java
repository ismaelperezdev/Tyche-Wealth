package com.tychewealth.service.helper.marketdata;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_CREATED_AT_EARLY;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_CREATED_AT_LATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.mapper.marketdata.PortfolioMarketDataMapper;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioMarketDataHelperTest {

  private static final Instant CAPTURED_AT = Instant.parse("2026-11-01T12:00:00Z");
  private static final Instant TO = Instant.parse("2026-11-02T00:00:00Z");

  @Mock private PortfolioMarketDataMapper portfolioMarketDataMapper;
  private PortfolioMarketDataHelper helper;

  @BeforeEach
  void setUp() {
    helper = new PortfolioMarketDataHelper(portfolioMarketDataMapper);
  }

  @Test
  void mapsLatestPricesToEachAsset() {
    AssetEntity asset = asset(" msft ", TEST_ASSET_CREATED_AT_EARLY);
    LatestMarketPrice price = new LatestMarketPrice("MSFT", BigDecimal.ONE, CAPTURED_AT);
    AssetMarketPriceDto expected =
        new AssetMarketPriceDto(
            1L, 1L, "MSFT", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, CAPTURED_AT);
    when(portfolioMarketDataMapper.toLatestPriceDto(asset, price)).thenReturn(expected);

    assertEquals(
        List.of(expected),
        helper.mapLatestPrices(List.of(asset), new MarketDataPage<>(List.of(price), 1)));
  }

  @Test
  void symbolsForTrimsUppercasesAndDeduplicatesSymbols() {
    assertEquals(
        List.of("AAPL", "MSFT"),
        helper.symbolsFor(
            List.of(
                asset(" aapl ", TEST_ASSET_CREATED_AT_EARLY),
                asset("MSFT", TEST_ASSET_CREATED_AT_EARLY),
                asset("Aapl", TEST_ASSET_CREATED_AT_LATE))));
  }

  @Test
  void clampsHistoryFromAndSkipsCheckpointBeforeAssetCreation() {
    AssetEntity earlierAsset = asset("aapl", TEST_ASSET_CREATED_AT_EARLY);
    AssetEntity laterAsset = asset(" aapl ", TEST_ASSET_CREATED_AT_LATE);
    Instant requestedFrom = Instant.parse("2026-09-01T00:00:00Z");
    Instant effectiveFrom = TEST_ASSET_CREATED_AT_EARLY.toInstant(ZoneOffset.UTC);
    Instant checkpointTime = Instant.parse("2026-10-10T00:00:00Z");
    Instant lateCreationTime = TEST_ASSET_CREATED_AT_LATE.toInstant(ZoneOffset.UTC);
    PriceCheckpoint beforeLateAsset =
        new PriceCheckpoint("AAPL", BigDecimal.ONE, checkpointTime, effectiveFrom);
    PriceCheckpoint atLateAssetCreation =
        new PriceCheckpoint("AAPL", BigDecimal.TEN, lateCreationTime, effectiveFrom);
    AssetPriceCheckpointDto earlyAssetCheckpoint =
        new AssetPriceCheckpointDto(
            1L, 1L, " aapl ", BigDecimal.ONE, checkpointTime, effectiveFrom);
    AssetPriceCheckpointDto earlyAssetAtLateCreation =
        new AssetPriceCheckpointDto(
            1L, 1L, " aapl ", BigDecimal.TEN, lateCreationTime, effectiveFrom);
    AssetPriceCheckpointDto lateAssetAtCreation =
        new AssetPriceCheckpointDto(
            2L, 2L, " aapl ", BigDecimal.TEN, lateCreationTime, effectiveFrom);
    when(portfolioMarketDataMapper.toCheckpointDto(earlierAsset, beforeLateAsset))
        .thenReturn(earlyAssetCheckpoint);
    when(portfolioMarketDataMapper.toCheckpointDto(earlierAsset, atLateAssetCreation))
        .thenReturn(earlyAssetAtLateCreation);
    when(portfolioMarketDataMapper.toCheckpointDto(laterAsset, atLateAssetCreation))
        .thenReturn(lateAssetAtCreation);
    List<AssetPriceCheckpointDto> result =
        helper.mapPriceHistory(
            List.of(earlierAsset, laterAsset),
            requestedFrom,
            new MarketDataPage<>(List.of(beforeLateAsset, atLateAssetCreation), 3));

    assertEquals(
        effectiveFrom,
        helper.effectiveHistoryFrom(List.of(earlierAsset, laterAsset), requestedFrom));
    assertEquals(
        List.of(earlyAssetCheckpoint, earlyAssetAtLateCreation, lateAssetAtCreation), result);
    verify(portfolioMarketDataMapper, never()).toCheckpointDto(laterAsset, beforeLateAsset);
  }

  @Test
  void effectiveHistoryFromUsesRequestedDateWhenAfterAssetCreation() {
    AssetEntity asset = asset("MSFT", TEST_ASSET_CREATED_AT_EARLY);
    Instant requestedFrom = Instant.parse("2026-11-01T00:00:00Z");

    assertEquals(requestedFrom, helper.effectiveHistoryFrom(List.of(asset), requestedFrom));
  }

  private static AssetEntity asset(String symbol, java.time.LocalDateTime createdAt) {
    AssetEntity asset = new AssetEntity();
    asset.setSymbol(symbol);
    asset.setCreatedAt(createdAt);
    return asset;
  }
}
