package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildAsset;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static com.tychewealth.testdata.PortfolioMarketDataTestData.CHECKPOINT_AT_LATER_ASSET_CREATION;
import static com.tychewealth.testdata.PortfolioMarketDataTestData.CHECKPOINT_BEFORE_LATER_ASSET;
import static com.tychewealth.testdata.PortfolioMarketDataTestData.EARLY_ASSET_CREATED_AT;
import static com.tychewealth.testdata.PortfolioMarketDataTestData.HISTORY_FROM;
import static com.tychewealth.testdata.PortfolioMarketDataTestData.HISTORY_TO;
import static com.tychewealth.testhelper.AuthTestHelper.createAccessToken;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.config.PortfolioMarketDataIntegrationTestConfig;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.marketdataclient.MarketDataClient;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = PortfolioMarketDataIntegrationTestConfig.class)
@ContextConfiguration(initializers = PortfolioMarketDataIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class PortfolioMarketDataServiceIntegrationTest {

  private static final String BASE_URL = PORTFOLIO_BASE_URL + "/market-data";
  private static final String TOKEN = createAccessToken(TEST_USER_ID);
  private static final String AUTHORIZATION = TOKEN_TYPE_BEARER_PREFIX + TOKEN;
  @Autowired private MockMvc mockMvc;
  @Autowired private AssetRepository assetRepository;
  @Autowired private PortfolioRepository portfolioRepository;
  @MockitoBean private MarketDataClient marketDataClient;

  private AssetEntity earlyAsset;
  private AssetEntity lateAsset;
  private PortfolioEntity ownPortfolio;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();

    ownPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            "Core",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.LONG);
    portfolioRepository.saveAndFlush(ownPortfolio);
    earlyAsset =
        buildAsset(ownPortfolio, "Apple Inc.", " aapl ", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD);
    assetRepository.saveAndFlush(earlyAsset);
    earlyAsset.setCreatedAt(LocalDateTime.ofInstant(EARLY_ASSET_CREATED_AT, ZoneOffset.UTC));
    assetRepository.saveAndFlush(earlyAsset);

    PortfolioEntity secondPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            "Retirement",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.LONG);
    portfolioRepository.saveAndFlush(secondPortfolio);
    lateAsset =
        buildAsset(
            secondPortfolio, "Apple Inc.", "AAPL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD);
    assetRepository.saveAndFlush(lateAsset);
    lateAsset.setCreatedAt(
        LocalDateTime.ofInstant(CHECKPOINT_AT_LATER_ASSET_CREATION, ZoneOffset.UTC));
    assetRepository.saveAndFlush(lateAsset);
  }

  @Test
  void latestUsesOwnedActiveAssetsAndSendsDistinctNormalizedSymbols() throws Exception {
    when(marketDataClient.getLatestPrices(List.of("AAPL"), 0, 5000, TOKEN))
        .thenReturn(
            new MarketDataPage<>(
                List.of(
                    new LatestMarketPrice(
                        "AAPL", new BigDecimal("225.50"), CHECKPOINT_AT_LATER_ASSET_CREATION)),
                1));

    mockMvc
        .perform(
            get(BASE_URL + "/latest")
                .param(
                    "portfolioIds",
                    ownPortfolio.getId().toString(),
                    lateAsset.getPortfolio().getId().toString())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].assetId").value(earlyAsset.getId()))
        .andExpect(jsonPath("$[0].symbol").value(" aapl "))
        .andExpect(jsonPath("$[0].marketPrice").value(225.50))
        .andExpect(jsonPath("$[1].assetId").value(lateAsset.getId()));

    verify(marketDataClient).getLatestPrices(List.of("AAPL"), 0, 5000, TOKEN);
  }

  @Test
  void historyClampsStartAndFiltersCheckpointsForEachAssetsCreation() throws Exception {
    PriceCheckpoint beforeLateAsset =
        new PriceCheckpoint("AAPL", BigDecimal.ONE, CHECKPOINT_BEFORE_LATER_ASSET, HISTORY_FROM);
    PriceCheckpoint atLateAssetCreation =
        new PriceCheckpoint(
            "AAPL", BigDecimal.TEN, CHECKPOINT_AT_LATER_ASSET_CREATION, HISTORY_FROM);
    Instant effectiveFrom = Instant.parse("2026-10-01T00:00:00Z");
    when(marketDataClient.getPriceHistory(
            List.of("AAPL"), effectiveFrom, HISTORY_TO, 0, 5000, TOKEN))
        .thenReturn(new MarketDataPage<>(List.of(beforeLateAsset, atLateAssetCreation), 2));

    mockMvc
        .perform(
            get(BASE_URL + "/history")
                .param("assetIds", earlyAsset.getId().toString(), lateAsset.getId().toString())
                .param("from", HISTORY_FROM.toString())
                .param("to", HISTORY_TO.toString())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].assetId").value(earlyAsset.getId()))
        .andExpect(jsonPath("$[0].price").value(1))
        .andExpect(jsonPath("$[1].assetId").value(earlyAsset.getId()))
        .andExpect(jsonPath("$[1].price").value(10))
        .andExpect(jsonPath("$[2].assetId").value(lateAsset.getId()))
        .andExpect(jsonPath("$[2].price").value(10));

    verify(marketDataClient)
        .getPriceHistory(List.of("AAPL"), effectiveFrom, HISTORY_TO, 0, 5000, TOKEN);
  }

  @Test
  void returnsUnprocessableEntityWhenMdsHistoryPageIsTruncated() throws Exception {
    PriceCheckpoint checkpoint =
        new PriceCheckpoint("AAPL", BigDecimal.ONE, CHECKPOINT_BEFORE_LATER_ASSET, HISTORY_FROM);
    when(marketDataClient.getPriceHistory(
            List.of("AAPL"), EARLY_ASSET_CREATED_AT, HISTORY_TO, 0, 5000, TOKEN))
        .thenReturn(new MarketDataPage<>(List.of(checkpoint), 2));

    mockMvc
        .perform(
            get(BASE_URL + "/history")
                .param("assetIds", earlyAsset.getId().toString())
                .param("from", HISTORY_FROM.toString())
                .param("to", HISTORY_TO.toString())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.type")
                .value(ErrorDefinition.PORTFOLIO_MARKET_DATA_RESULT_LIMIT_EXCEEDED.getType()));
  }

  @Test
  void rejectsAnotherUsersPortfolioBeforeCallingMds() throws Exception {
    PortfolioEntity anotherUsersPortfolio =
        buildPortfolio(
            TEST_USER_ID + 1,
            "Other",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.LONG);
    portfolioRepository.saveAndFlush(anotherUsersPortfolio);

    mockMvc
        .perform(
            get(BASE_URL + "/latest")
                .param("portfolioIds", anotherUsersPortfolio.getId().toString())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION))
        .andExpect(status().isNotFound());

    verify(marketDataClient, never())
        .getLatestPrices(
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void rejectsLatestResultsAboveTheConfiguredLimit() throws Exception {
    when(marketDataClient.getLatestPrices(List.of("AAPL"), 0, 5000, TOKEN))
        .thenReturn(
            new MarketDataPage<>(
                List.of(new LatestMarketPrice("AAPL", BigDecimal.ONE, HISTORY_FROM)), 5001));

    mockMvc
        .perform(
            get(BASE_URL + "/latest")
                .param("assetIds", earlyAsset.getId().toString())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.type")
                .value(ErrorDefinition.PORTFOLIO_MARKET_DATA_RESULT_LIMIT_EXCEEDED.getType()));
  }
}
