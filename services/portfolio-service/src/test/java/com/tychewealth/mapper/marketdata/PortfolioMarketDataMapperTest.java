package com.tychewealth.mapper.marketdata;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_AVERAGE_PRICE_400;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_QUANTITY_TWO;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_BUCKET_START;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_CAPTURED_AT;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_PRICE_425_50;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PortfolioMarketDataMapperTest {

  private final PortfolioMarketDataMapper mapper =
      Mappers.getMapper(PortfolioMarketDataMapper.class);

  @Test
  void mapsLatestPriceWithAssetAndPortfolioFields() {
    AssetEntity asset = createAsset();
    AssetMarketPriceDto dto =
        mapper.toLatestPriceDto(
            asset,
            new LatestMarketPrice(
                TEST_ASSET_SYMBOL_MSFT, TEST_MARKET_PRICE_425_50, TEST_MARKET_DATA_CAPTURED_AT));

    assertEquals(TEST_ASSET_ID, dto.assetId());
    assertEquals(TEST_PORTFOLIO_ID, dto.portfolioId());
    assertEquals(TEST_ASSET_SYMBOL_MSFT, dto.symbol());
    assertEquals(TEST_ASSET_QUANTITY_TWO, dto.quantity());
    assertEquals(TEST_ASSET_AVERAGE_PRICE_400, dto.averagePrice());
    assertEquals(TEST_MARKET_PRICE_425_50, dto.marketPrice());
    assertEquals(TEST_MARKET_DATA_CAPTURED_AT, dto.capturedAt());
  }

  @Test
  void mapsCheckpointWithAssetAndPortfolioFields() {
    AssetEntity asset = createAsset();
    AssetPriceCheckpointDto dto =
        mapper.toCheckpointDto(
            asset,
            new PriceCheckpoint(
                TEST_ASSET_SYMBOL_MSFT,
                TEST_MARKET_PRICE_425_50,
                TEST_MARKET_DATA_CAPTURED_AT,
                TEST_MARKET_DATA_BUCKET_START));

    assertEquals(TEST_ASSET_ID, dto.assetId());
    assertEquals(TEST_PORTFOLIO_ID, dto.portfolioId());
    assertEquals(TEST_ASSET_SYMBOL_MSFT, dto.symbol());
    assertEquals(TEST_MARKET_PRICE_425_50, dto.price());
    assertEquals(TEST_MARKET_DATA_CAPTURED_AT, dto.capturedAt());
    assertEquals(TEST_MARKET_DATA_BUCKET_START, dto.bucketStart());
  }

  private static AssetEntity createAsset() {
    PortfolioEntity portfolio = new PortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);

    AssetEntity asset = new AssetEntity();
    asset.setId(TEST_ASSET_ID);
    asset.setPortfolio(portfolio);
    asset.setSymbol(TEST_ASSET_SYMBOL_MSFT);
    asset.setQuantity(TEST_ASSET_QUANTITY_TWO);
    asset.setAveragePrice(TEST_ASSET_AVERAGE_PRICE_400);
    return asset;
  }
}
