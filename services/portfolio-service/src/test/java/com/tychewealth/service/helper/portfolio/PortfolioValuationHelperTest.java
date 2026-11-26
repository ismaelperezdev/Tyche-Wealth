package com.tychewealth.service.helper.portfolio;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_VALUATION_ACQUISITION_COST_180_00;
import static com.tychewealth.constants.TestConstants.TEST_VALUATION_PROFIT_LOSS_671_00;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_VALUATION_EXCHANGE_RATE_MISSING;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_VALUATION_MARKET_PRICE_MISSING;
import static com.tychewealth.testdata.AssetVariationTestData.createdVariation;
import static com.tychewealth.testdata.AssetVariationTestData.deletedVariation;
import static com.tychewealth.testdata.AssetVariationTestData.manualUpdateVariation;
import static com.tychewealth.testdata.PortfolioValuationTestData.asset;
import static com.tychewealth.testdata.PortfolioValuationTestData.marketPriceCheckpoint;
import static com.tychewealth.testdata.PortfolioValuationTestData.positionSnapshot;
import static com.tychewealth.testdata.PortfolioValuationTestData.usdToEurExchangeRate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.dto.portfolio.PortfolioValuationPriceSource;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.model.PortfolioPositionSnapshot;
import com.tychewealth.repository.AssetVariationRepository;
import com.tychewealth.service.helper.marketdata.PortfolioValuationMarketDataHelper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PortfolioValuationHelperTest {

  private final PortfolioValuationHelper valuationHelper =
      new PortfolioValuationHelper(
          Mappers.getMapper(PortfolioMapper.class),
          org.mockito.Mockito.mock(AssetVariationRepository.class),
          org.mockito.Mockito.mock(PortfolioValuationMarketDataHelper.class));

  @Test
  void defaultsMissingToToCurrentTime() {
    Instant before = Instant.now();

    Instant effectiveTo = valuationHelper.resolveTo(null);

    Instant after = Instant.now();
    assertFalse(effectiveTo.isBefore(before));
    assertFalse(effectiveTo.isAfter(after));
  }

  @Test
  void preservesProvidedTo() {
    Instant to = Instant.parse("2026-11-02T00:00:00Z");

    assertEquals(to, valuationHelper.resolveTo(to));
  }

  @Test
  void defaultsMissingFromToPortfolioCreationTime() {
    PortfolioEntity portfolio = new PortfolioEntity();
    LocalDateTime createdAt = LocalDateTime.parse("2026-01-15T10:30:00");
    portfolio.setCreatedAt(createdAt);

    assertEquals(createdAt.toInstant(ZoneOffset.UTC), valuationHelper.resolveFrom(null, portfolio));
  }

  @Test
  void preservesProvidedFrom() {
    Instant from = Instant.parse("2026-01-01T00:00:00Z");

    assertEquals(from, valuationHelper.resolveFrom(from, new PortfolioEntity()));
  }

  @Test
  void reconstructsPositionsAtEachTimestampIncludingBoundaryChangesAndDeletion() {
    AssetEntity keptAsset = asset(1L, "KEEP");
    AssetEntity deletedAsset = asset(2L, "REMOVE");
    LocalDateTime initialAt = LocalDateTime.parse("2026-11-01T00:00:00");
    LocalDateTime updateAt = LocalDateTime.parse("2026-11-02T00:00:00");
    LocalDateTime deleteAt = LocalDateTime.parse("2026-11-03T00:00:00");
    List<PortfolioPositionSnapshot> snapshots =
        valuationHelper.reconstructPositions(
            List.of(
                createdVariation(keptAsset, "10", "100", initialAt),
                createdVariation(deletedAsset, "4", "50", initialAt)),
            List.of(
                manualUpdateVariation(keptAsset, "10", "12", "100", "110", updateAt),
                deletedVariation(deletedAsset, "4", "50", deleteAt)),
            List.of(
                deleteAt.toInstant(ZoneOffset.UTC),
                initialAt.toInstant(ZoneOffset.UTC),
                updateAt.toInstant(ZoneOffset.UTC)));

    assertEquals(3, snapshots.size());
    assertEquals(initialAt.toInstant(ZoneOffset.UTC), snapshots.getFirst().timestamp());
    assertEquals(2, snapshots.get(0).positions().size());
    assertEquals(new BigDecimal("10"), snapshots.get(0).positions().getFirst().quantity());
    assertEquals(updateAt.toInstant(ZoneOffset.UTC), snapshots.get(1).timestamp());
    assertEquals(2, snapshots.get(1).positions().size());
    assertEquals(new BigDecimal("12"), snapshots.get(1).positions().getFirst().quantity());
    assertEquals(new BigDecimal("110"), snapshots.get(1).positions().getFirst().averagePrice());
    assertEquals(deleteAt.toInstant(ZoneOffset.UTC), snapshots.get(2).timestamp());
    assertEquals(1, snapshots.get(2).positions().size());
    assertEquals(keptAsset.getId(), snapshots.get(2).positions().getFirst().assetId());
  }

  @Test
  void returnsNoSnapshotsWhenThereAreNoTimestamps() {
    assertEquals(List.of(), valuationHelper.reconstructPositions(List.of(), List.of(), List.of()));
  }

  @Test
  void convertsMarketPriceAndAcquisitionCostUsingExchangeRate() {
    var point =
        valuationHelper
            .calculateValuationPoints(
                List.of(positionSnapshot(CurrencyCodeEnum.USD)),
                List.of(),
                Map.of(),
                Map.of(TEST_ASSET_SYMBOL_MSFT, marketPriceCheckpoint()),
                Map.of(
                    PortfolioValuationHelper.exchangeRateKey("USD", "EUR"), usdToEurExchangeRate()),
                CurrencyCodeEnum.EUR,
                false)
            .getFirst();

    assertEquals(0, TEST_VALUATION_PROFIT_LOSS_671_00.compareTo(point.unrealizedProfitLoss()));
    assertEquals(0, TEST_VALUATION_ACQUISITION_COST_180_00.compareTo(point.acquisitionCost()));
    assertEquals(1, point.priceObservations().size());
    assertEquals(TEST_ASSET_SYMBOL_MSFT, point.priceObservations().getFirst().symbol());
    assertEquals(
        PortfolioValuationPriceSource.CHECKPOINT, point.priceObservations().getFirst().source());
    assertEquals(
        marketPriceCheckpoint().capturedAt(), point.priceObservations().getFirst().capturedAt());
  }

  @Test
  void throwsWhenMarketPriceIsMissing() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                valuationHelper.calculateValuationPoints(
                    List.of(positionSnapshot(CurrencyCodeEnum.EUR)),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    CurrencyCodeEnum.EUR,
                    false));

    assertEquals(PORTFOLIO_VALUATION_MARKET_PRICE_MISSING, exception.getErrorDefinition());
  }

  @Test
  void throwsWhenExchangeRateIsMissing() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                valuationHelper.calculateValuationPoints(
                    List.of(positionSnapshot(CurrencyCodeEnum.USD)),
                    List.of(),
                    Map.of(),
                    Map.of(TEST_ASSET_SYMBOL_MSFT, marketPriceCheckpoint()),
                    Map.of(),
                    CurrencyCodeEnum.EUR,
                    false));

    assertEquals(PORTFOLIO_VALUATION_EXCHANGE_RATE_MISSING, exception.getErrorDefinition());
  }
}
