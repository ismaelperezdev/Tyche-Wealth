package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.CommonConstants.BASE_CURRENCY;
import static com.tychewealth.constants.CommonConstants.DESCRIPTION;
import static com.tychewealth.constants.CommonConstants.INVESTMENT_HORIZON;
import static com.tychewealth.constants.CommonConstants.MAX_RISK;
import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.CommonConstants.NAME_PLACEHOLDER;
import static com.tychewealth.constants.CommonConstants.RISK_PROFILE;
import static com.tychewealth.constants.CommonConstants.STRATEGY_TYPE;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_USD;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_MEDIUM;
import static com.tychewealth.constants.TestConstants.TEST_JSON_CODE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_JSON_TYPE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_MARKET_DATA_SERVICE_REGISTRATION;
import static com.tychewealth.constants.TestConstants.TEST_MAX_PORTFOLIOS_PER_USER;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_ANOTHER;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_LONG_TERM;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_OTHER_OWNER;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_CORE;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_OTHER_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_SERVICE_PRINCIPAL;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_MEDIUM;
import static com.tychewealth.constants.TestConstants.TEST_SERVICE_ACCESS_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_BALANCED;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_X_HAS_NEXT_HEADER;
import static com.tychewealth.constants.TestConstants.TEST_X_LIMIT_HEADER;
import static com.tychewealth.constants.TestConstants.TEST_X_PAGE_HEADER;
import static com.tychewealth.constants.TestConstants.TEST_X_TOTAL_COUNT_HEADER;
import static com.tychewealth.testdata.AssetVariationTestData.createdVariation;
import static com.tychewealth.testdata.EntityBuilder.buildAsset;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static com.tychewealth.testdata.PortfolioTestData.invalidEnumCreateRequest;
import static com.tychewealth.testdata.PortfolioTestData.malformedCreateRequest;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;
import static com.tychewealth.testhelper.PortfolioTestHelper.deleteMeRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.retrieveMeByIdRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.retrieveMeRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.updateMeRequest;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.config.PortfolioIntegrationTestConfig;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.marketdataclient.MarketDataClient;
import com.tychewealth.marketdataclient.dto.ExchangeRate;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.AssetVariationRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.assetvariation.AssetVariationService;
import com.tychewealth.serviceauth.client.ServiceAccessTokenProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(classes = PortfolioIntegrationTestConfig.class)
@ContextConfiguration(initializers = PortfolioIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class PortfolioApiControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;
  @Autowired private AssetVariationRepository assetVariationRepository;

  @MockitoBean private AssetVariationService assetVariationService;
  @MockitoBean private MarketDataClient marketDataClient;
  @MockitoBean private ServiceAccessTokenProvider serviceAccessTokenProvider;

  @BeforeEach
  void setUp() {
    assetVariationRepository.deleteAll();
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();
  }

  @Test
  void valuationCombinesPersistedPositionWithLatestMarketPrice() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_CORE,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.LONG));
    AssetEntity asset =
        assetRepository.saveAndFlush(
            buildAsset(
                portfolio,
                "Microsoft",
                TEST_ASSET_SYMBOL_MSFT,
                AssetTypeEnum.STOCK,
                CurrencyCodeEnum.USD));

    Instant from = Instant.now().minusSeconds(60);
    assetVariationRepository.saveAndFlush(
        createdVariation(
            asset,
            "10.00",
            "100.00",
            LocalDateTime.ofInstant(from.plusSeconds(1), ZoneOffset.UTC)));

    PriceCheckpoint initialCheckpoint =
        new PriceCheckpoint(TEST_ASSET_SYMBOL_MSFT, new BigDecimal("100.00"), from, from);
    LatestMarketPrice latestPrice =
        new LatestMarketPrice(TEST_ASSET_SYMBOL_MSFT, new BigDecimal("120.00"), Instant.now());
    when(serviceAccessTokenProvider.getAccessToken(
            TEST_MARKET_DATA_SERVICE_REGISTRATION, TEST_PORTFOLIO_SERVICE_PRINCIPAL))
        .thenReturn(TEST_SERVICE_ACCESS_TOKEN);
    when(marketDataClient.retrievePriceCheckpointsForInstants(
            eq(List.of(TEST_ASSET_SYMBOL_MSFT)), anyList(), eq(TEST_SERVICE_ACCESS_TOKEN)))
        .thenAnswer(
            invocation -> {
              List<Instant> requestedCapturedAts = invocation.getArgument(1);
              return requestedCapturedAts.stream()
                  .map(
                      requestedCapturedAt ->
                          new PriceCheckpoint(
                              initialCheckpoint.symbol(),
                              initialCheckpoint.price(),
                              initialCheckpoint.capturedAt(),
                              initialCheckpoint.bucketStart(),
                              requestedCapturedAt))
                  .toList();
            });
    when(marketDataClient.getSampledPriceHistory(
            eq(List.of(TEST_ASSET_SYMBOL_MSFT)),
            eq(from),
            any(Instant.class),
            eq(TEST_SERVICE_ACCESS_TOKEN)))
        .thenReturn(List.of());
    when(marketDataClient.getLatestPrices(
            List.of(TEST_ASSET_SYMBOL_MSFT), 0, 1, TEST_SERVICE_ACCESS_TOKEN))
        .thenReturn(new MarketDataPage<>(List.of(latestPrice), 1));
    LocalDate fxEffectiveDate = LocalDate.of(2026, 11, 25);
    when(marketDataClient.getExchangeRates(
            List.of("USD"), List.of("EUR"), TEST_SERVICE_ACCESS_TOKEN))
        .thenReturn(
            List.of(new ExchangeRate("USD", "EUR", new BigDecimal("0.90"), fxEffectiveDate)));

    mockMvc
        .perform(
            get(PORTFOLIO_BASE_URL + "/me/{portfolioId}/valuation", portfolio.getId())
                .param("from", from.toString())
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.baseCurrency").value(TEST_BASE_CURRENCY_EUR))
        .andExpect(jsonPath("$.points", hasSize(3)))
        .andExpect(
            jsonPath("$.points[1].priceObservations[0].symbol").value(TEST_ASSET_SYMBOL_MSFT))
        .andExpect(jsonPath("$.points[1].priceObservations[0].source").value("CHECKPOINT"))
        .andExpect(jsonPath("$.points[1].priceObservations[0].capturedAt").value(from.toString()))
        .andExpect(jsonPath("$.points[2].priceObservations[0].source").value("LATEST"))
        .andExpect(
            jsonPath("$.points[2].priceObservations[0].capturedAt")
                .value(latestPrice.capturedAt().toString()))
        .andExpect(jsonPath("$.points[2].unrealizedProfitLoss").value(300.0))
        .andExpect(jsonPath("$.points[2].acquisitionCost").value(900.0))
        .andExpect(jsonPath("$.fxRates", hasSize(1)))
        .andExpect(jsonPath("$.fxRates[0].baseCurrency").value("USD"))
        .andExpect(jsonPath("$.fxRates[0].quoteCurrency").value("EUR"))
        .andExpect(jsonPath("$.fxRates[0].effectiveDate").value(fxEffectiveDate.toString()));
  }

  @Test
  void valuationUsesCheckpointBeforeAcquisitionBetweenTwoCheckpoints() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_CORE,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.LONG));
    AssetEntity asset =
        assetRepository.saveAndFlush(
            buildAsset(
                portfolio,
                "Microsoft",
                TEST_ASSET_SYMBOL_MSFT,
                AssetTypeEnum.STOCK,
                CurrencyCodeEnum.EUR));

    Instant from = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(1);
    Instant earlierCheckpointAt = from.plusSeconds(10);
    Instant acquisitionAt = from.plusSeconds(30);
    Instant laterCheckpointAt = from.plusSeconds(50);
    Instant to = from.plusSeconds(90);
    assetVariationRepository.saveAndFlush(
        createdVariation(
            asset, "10.00", "100.00", LocalDateTime.ofInstant(acquisitionAt, ZoneOffset.UTC)));
    PriceCheckpoint earlierCheckpoint =
        new PriceCheckpoint(
            TEST_ASSET_SYMBOL_MSFT,
            new BigDecimal("120.00"),
            earlierCheckpointAt,
            earlierCheckpointAt);
    PriceCheckpoint laterCheckpoint =
        new PriceCheckpoint(
            TEST_ASSET_SYMBOL_MSFT, new BigDecimal("130.00"), laterCheckpointAt, laterCheckpointAt);

    when(serviceAccessTokenProvider.getAccessToken(
            TEST_MARKET_DATA_SERVICE_REGISTRATION, TEST_PORTFOLIO_SERVICE_PRINCIPAL))
        .thenReturn(TEST_SERVICE_ACCESS_TOKEN);
    when(marketDataClient.getSampledPriceHistory(
            eq(List.of(TEST_ASSET_SYMBOL_MSFT)), eq(from), eq(to), eq(TEST_SERVICE_ACCESS_TOKEN)))
        .thenReturn(List.of(earlierCheckpoint, laterCheckpoint));
    when(marketDataClient.retrievePriceCheckpointsForInstants(
            eq(List.of(TEST_ASSET_SYMBOL_MSFT)), anyList(), eq(TEST_SERVICE_ACCESS_TOKEN)))
        .thenAnswer(
            invocation -> {
              List<Instant> requestedCapturedAts = invocation.getArgument(1);
              assertTrue(requestedCapturedAts.contains(acquisitionAt));
              return requestedCapturedAts.stream()
                  .map(
                      requestedCapturedAt ->
                          List.of(earlierCheckpoint, laterCheckpoint).stream()
                              .filter(
                                  checkpoint ->
                                      !checkpoint.capturedAt().isAfter(requestedCapturedAt))
                              .reduce((first, second) -> second)
                              .map(
                                  checkpoint ->
                                      new PriceCheckpoint(
                                          checkpoint.symbol(),
                                          checkpoint.price(),
                                          checkpoint.capturedAt(),
                                          checkpoint.bucketStart(),
                                          requestedCapturedAt)))
                  .flatMap(java.util.Optional::stream)
                  .toList();
            });
    when(marketDataClient.retrievePriceCheckpoints(
            List.of(TEST_ASSET_SYMBOL_MSFT), to, TEST_SERVICE_ACCESS_TOKEN))
        .thenReturn(List.of(laterCheckpoint));

    mockMvc
        .perform(
            get(PORTFOLIO_BASE_URL + "/me/{portfolioId}/valuation", portfolio.getId())
                .param("from", from.toString())
                .param("to", to.toString())
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points", hasSize(5)))
        .andExpect(jsonPath("$.points[2].timestamp").value(acquisitionAt.toString()))
        .andExpect(
            jsonPath("$.points[2].priceObservations[0].capturedAt")
                .value(earlierCheckpointAt.toString()))
        .andExpect(jsonPath("$.points[2].unrealizedProfitLoss").value(200.0));
  }

  @Test
  void persistsAssetIdentityOnVariationIndependentlyOfLaterAssetUpdates() {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_CORE,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.LONG));
    AssetEntity asset =
        assetRepository.saveAndFlush(
            buildAsset(
                portfolio,
                "Microsoft",
                TEST_ASSET_SYMBOL_MSFT,
                AssetTypeEnum.STOCK,
                CurrencyCodeEnum.USD));
    LocalDateTime occurredAt = LocalDateTime.of(2026, 11, 1, 12, 0);
    AssetVariationEntity variation =
        assetVariationRepository.saveAndFlush(
            createdVariation(asset, "10.00", "100.00", occurredAt));

    asset.setSymbol("MSFT-NEW");
    asset.setCurrency(CurrencyCodeEnum.GBP);
    assetRepository.saveAndFlush(asset);

    AssetVariationEntity persistedVariation =
        assetVariationRepository.findById(variation.getId()).orElseThrow();
    assertEquals(TEST_ASSET_SYMBOL_MSFT, persistedVariation.getSymbol());
    assertEquals(CurrencyCodeEnum.USD, persistedVariation.getCurrency());
  }

  @Test
  void retrieveReturnsOkWithAuthenticatedUserPortfoliosOnly() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_CORE,
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.MEDIUM));
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_OTHER_USER_ID,
            "Other User Portfolio",
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    retrieveMeRequest(mockMvc, String.valueOf(TEST_USER_ID))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string(TEST_X_TOTAL_COUNT_HEADER, "2"))
        .andExpect(header().string(TEST_X_PAGE_HEADER, "0"))
        .andExpect(header().string(TEST_X_LIMIT_HEADER, "10"))
        .andExpect(header().string(TEST_X_HAS_NEXT_HEADER, "false"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value(TEST_PORTFOLIO_NAME_CORE))
        .andExpect(jsonPath("$[1].name").value(TEST_PORTFOLIO_NAME_RETIREMENT));
  }

  @Test
  void retrieveReturnsPagedSliceWhenPageAndLimitAreProvided() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_CORE,
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.MEDIUM));
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    retrieveMeRequest(mockMvc, String.valueOf(TEST_USER_ID), 1, 1)
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string(TEST_X_TOTAL_COUNT_HEADER, "2"))
        .andExpect(header().string(TEST_X_PAGE_HEADER, "1"))
        .andExpect(header().string(TEST_X_LIMIT_HEADER, "1"))
        .andExpect(header().string(TEST_X_HAS_NEXT_HEADER, "false"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value(TEST_PORTFOLIO_NAME_RETIREMENT));
  }

  @Test
  void retrieveReturnsEmptyListWhenAuthenticatedUserHasNoPortfolios() throws Exception {
    retrieveMeRequest(mockMvc, String.valueOf(TEST_USER_ID))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string(TEST_X_TOTAL_COUNT_HEADER, "0"))
        .andExpect(header().string(TEST_X_PAGE_HEADER, "0"))
        .andExpect(header().string(TEST_X_LIMIT_HEADER, "10"))
        .andExpect(header().string(TEST_X_HAS_NEXT_HEADER, "false"))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void retrieveReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    retrieveMeRequest(mockMvc, null)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION).value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void retrieveByIdReturnsOkWhenPortfolioBelongsToAuthenticatedUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retrieveMeByIdRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId())
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").value(portfolio.getId()))
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_RETIREMENT))
        .andExpect(jsonPath("$." + BASE_CURRENCY).value(TEST_BASE_CURRENCY_EUR));
  }

  @Test
  void retrieveByIdReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retrieveMeByIdRequest(mockMvc, null, portfolio.getId())
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()));
  }

  @Test
  void retrieveByIdReturnsNotFoundWhenPortfolioBelongsToAnotherUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_OTHER_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retrieveMeByIdRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId())
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getDescription()));
  }

  @Test
  void createReturnsCreatedWhenPayloadIsValid() throws Exception {
    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_RETIREMENT))
        .andExpect(jsonPath("$." + DESCRIPTION).value(TEST_PORTFOLIO_DESCRIPTION_LONG_TERM))
        .andExpect(jsonPath("$." + BASE_CURRENCY).value(TEST_BASE_CURRENCY_EUR))
        .andExpect(jsonPath("$." + RISK_PROFILE).value(TEST_RISK_PROFILE_LOW))
        .andExpect(jsonPath("$." + INVESTMENT_HORIZON).value(TEST_INVESTMENT_HORIZON_LONG))
        .andExpect(jsonPath("$." + STRATEGY_TYPE).value(TEST_STRATEGY_TYPE_INCOME))
        .andExpect(jsonPath("$." + MAX_RISK).value(0.40))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());

    List<PortfolioEntity> portfolios =
        portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID);
    assertEquals(1, portfolios.size());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, portfolios.getFirst().getName());
    assertNotNull(portfolios.getFirst().getCreatedAt());
  }

  @Test
  void createReturnsBadRequestWhenAuthenticatedUserIsMissing() throws Exception {
    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, null, requestBody)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION).value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.PortfolioTestData#invalidCreateRequests")
  void createReturnsBadRequestForInvalidPayload(String requestBody, String expectedMessage)
      throws Exception {
    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString(expectedMessage)));
  }

  @Test
  void createReturnsBadRequestForMalformedJson() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(PORTFOLIO_BASE_URL)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedCreateRequest()))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("invalid")));
  }

  @Test
  void createReturnsBadRequestForInvalidEnumValue() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(PORTFOLIO_BASE_URL)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEnumCreateRequest()))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("field 'baseCurrency'")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("option 'INVALID'")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("Available options:")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("EUR")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("USD")));
  }

  @Test
  void createReturnsConflictWhenPortfolioNameAlreadyExistsForUser() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(
                    ErrorDefinition.PORTFOLIO_NAME_CONFLICT
                        .getDescription()
                        .replace(NAME_PLACEHOLDER, TEST_PORTFOLIO_NAME_RETIREMENT)));
  }

  @Test
  void createReturnsConflictWhenUserAlreadyHasMaximumPortfolios() throws Exception {
    for (int index = 0; index < TEST_MAX_PORTFOLIOS_PER_USER; index++) {
      portfolioRepository.saveAndFlush(
          buildPortfolio(
              TEST_USER_ID,
              "Portfolio " + index,
              CurrencyCodeEnum.EUR,
              RiskProfileEnum.LOW,
              StrategyTypeEnum.INCOME,
              InvestmentHorizonEnum.LONG));
    }

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_LIMIT_REACHED.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_LIMIT_REACHED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.PORTFOLIO_LIMIT_REACHED.getDescription()));
  }

  @Test
  void createAllowsSamePortfolioNameForDifferentUsers() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_OTHER_OWNER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_OTHER_USER_ID), requestBody)
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_RETIREMENT));

    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID).size());
    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_OTHER_USER_ID).size());
  }

  @Test
  void updateReturnsOkWhenPortfolioBelongsToAuthenticatedUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_CORE,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId(), requestBody)
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").value(portfolio.getId()))
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_CORE))
        .andExpect(jsonPath("$." + DESCRIPTION).value(TEST_PORTFOLIO_DESCRIPTION_ANOTHER))
        .andExpect(jsonPath("$." + BASE_CURRENCY).value(TEST_BASE_CURRENCY_USD))
        .andExpect(jsonPath("$." + RISK_PROFILE).value(TEST_RISK_PROFILE_MEDIUM))
        .andExpect(jsonPath("$." + INVESTMENT_HORIZON).value(TEST_INVESTMENT_HORIZON_MEDIUM))
        .andExpect(jsonPath("$." + STRATEGY_TYPE).value(TEST_STRATEGY_TYPE_BALANCED))
        .andExpect(jsonPath("$." + MAX_RISK).value(0.3));

    PortfolioEntity updatedPortfolio =
        portfolioRepository.findById(portfolio.getId()).orElseThrow();
    assertEquals(TEST_PORTFOLIO_NAME_CORE, updatedPortfolio.getName());
    assertEquals(TEST_PORTFOLIO_DESCRIPTION_ANOTHER, updatedPortfolio.getDescription());
    assertEquals(CurrencyCodeEnum.USD, updatedPortfolio.getBaseCurrency());
  }

  @Test
  void updateReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_CORE,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, null, portfolio.getId(), requestBody)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertEquals(
        TEST_PORTFOLIO_NAME_RETIREMENT,
        portfolioRepository.findById(portfolio.getId()).orElseThrow().getName());
  }

  @Test
  void updateReturnsNotFoundWhenPortfolioBelongsToAnotherUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_OTHER_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_CORE,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId(), requestBody)
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getDescription()));
  }

  @Test
  void updateReturnsConflictWhenPortfolioNameAlreadyExistsForUser() throws Exception {
    PortfolioEntity existingPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_CORE,
                CurrencyCodeEnum.USD,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.MEDIUM));
    PortfolioEntity targetPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            existingPortfolio.getName(),
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, String.valueOf(TEST_USER_ID), targetPortfolio.getId(), requestBody)
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getType()));
  }

  @Test
  void deleteReturnsNoContentAndRemovesOwnedPortfolioWithAssets() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));
    assetRepository.saveAndFlush(
        buildAsset(portfolio, "Apple Inc.", "AAPL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    deleteMeRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId())
        .andExpect(status().isNoContent())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertEquals(0, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID).size());
    assertEquals(0, assetRepository.findByPortfolioId(portfolio.getId()).size());
  }

  @Test
  void deleteReturnsNoContentWhenPortfolioBelongsToAnotherUser() throws Exception {
    PortfolioEntity otherUsersPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_OTHER_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    deleteMeRequest(mockMvc, String.valueOf(TEST_USER_ID), otherUsersPortfolio.getId())
        .andExpect(status().isNoContent())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_OTHER_USER_ID).size());
  }

  @Test
  void deleteReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    deleteMeRequest(mockMvc, null, portfolio.getId())
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertTrue(portfolioRepository.findById(portfolio.getId()).isPresent());
  }
}
