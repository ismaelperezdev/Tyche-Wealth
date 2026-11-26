package com.tychewealth.service.helper.marketdata;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_BUCKET_START;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_CAPTURED_AT;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_SERVICE_REGISTRATION;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_PRICE_25_00;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_PRICE_425_50;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_SERVICE_PRINCIPAL;
import static com.tychewealth.constants.TestConstants.TEST_SERVICE_ACCESS_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.marketdataclient.MarketDataClient;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.serviceauth.client.ServiceAccessTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioValuationMarketDataHelperTest {

  @Mock private MarketDataClient marketDataClient;
  @Mock private ServiceAccessTokenProvider serviceAccessTokenProvider;

  @InjectMocks private PortfolioValuationMarketDataHelper marketDataHelper;

  @Test
  void retrievesCheckpointsOnlyForSymbolsMissingLatestPrices() {
    var capturedAt = TEST_MARKET_DATA_CAPTURED_AT;
    LatestMarketPrice latestPrice =
        new LatestMarketPrice(TEST_ASSET_SYMBOL_AAPL, TEST_MARKET_PRICE_425_50, capturedAt);
    PriceCheckpoint fallbackCheckpoint =
        new PriceCheckpoint(
            TEST_ASSET_SYMBOL_MSFT,
            TEST_MARKET_PRICE_25_00,
            capturedAt,
            TEST_MARKET_DATA_BUCKET_START);
    when(serviceAccessTokenProvider.getAccessToken(
            TEST_MARKET_DATA_SERVICE_REGISTRATION, TEST_PORTFOLIO_SERVICE_PRINCIPAL))
        .thenReturn(TEST_SERVICE_ACCESS_TOKEN);
    when(marketDataClient.getLatestPrices(
            List.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT),
            0,
            2,
            TEST_SERVICE_ACCESS_TOKEN))
        .thenReturn(new MarketDataPage<>(List.of(latestPrice), 1));
    when(marketDataClient.retrievePriceCheckpoints(
            List.of(TEST_ASSET_SYMBOL_MSFT), capturedAt, TEST_SERVICE_ACCESS_TOKEN))
        .thenReturn(List.of(fallbackCheckpoint));

    var result =
        marketDataHelper.retrieveEndPrices(
            List.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT), capturedAt, true);

    assertThat(result.latestPrices()).containsOnlyKeys(TEST_ASSET_SYMBOL_AAPL);
    assertThat(result.checkpoints()).containsOnlyKeys(TEST_ASSET_SYMBOL_MSFT);
    verify(marketDataClient)
        .retrievePriceCheckpoints(
            List.of(TEST_ASSET_SYMBOL_MSFT), capturedAt, TEST_SERVICE_ACCESS_TOKEN);
  }
}
