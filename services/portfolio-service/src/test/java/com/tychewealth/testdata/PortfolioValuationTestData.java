package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_QUANTITY_TWO;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_AVERAGE_PRICE_100_00;
import static com.tychewealth.constants.TestConstants.TEST_EXCHANGE_RATE_0_90;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_CAPTURED_AT;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_PRICE_425_50;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.marketdataclient.dto.ExchangeRate;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.model.PortfolioPositionSnapshot;
import com.tychewealth.model.PortfolioValuationPosition;
import java.time.ZoneOffset;
import java.util.List;

/** Test fixtures for historical portfolio valuation. */
public final class PortfolioValuationTestData {

  private PortfolioValuationTestData() {}

  public static AssetEntity asset(Long id, String symbol) {
    AssetEntity asset = new AssetEntity();
    asset.setId(id);
    asset.setSymbol(symbol);
    asset.setCurrency(CurrencyCodeEnum.USD);
    return asset;
  }

  public static PortfolioPositionSnapshot positionSnapshot(CurrencyCodeEnum currency) {
    return new PortfolioPositionSnapshot(
        TEST_MARKET_DATA_CAPTURED_AT,
        List.of(
            new PortfolioValuationPosition(
                1L,
                TEST_ASSET_SYMBOL_MSFT,
                currency,
                TEST_ASSET_QUANTITY_TWO,
                TEST_AVERAGE_PRICE_100_00)));
  }

  public static PriceCheckpoint marketPriceCheckpoint() {
    return new PriceCheckpoint(
        TEST_ASSET_SYMBOL_MSFT,
        TEST_MARKET_PRICE_425_50,
        TEST_MARKET_DATA_CAPTURED_AT,
        TEST_MARKET_DATA_CAPTURED_AT);
  }

  public static ExchangeRate usdToEurExchangeRate() {
    return new ExchangeRate(
        "USD",
        "EUR",
        TEST_EXCHANGE_RATE_0_90,
        TEST_MARKET_DATA_CAPTURED_AT.atZone(ZoneOffset.UTC).toLocalDate());
  }
}
