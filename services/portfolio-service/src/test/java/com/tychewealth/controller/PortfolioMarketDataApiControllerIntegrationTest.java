package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.config.PortfolioMarketDataIntegrationTestConfig;
import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.service.PortfolioMarketDataService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class PortfolioMarketDataApiControllerIntegrationTest {

  private static final String BASE_URL = PORTFOLIO_BASE_URL + "/market-data";
  private static final Instant CAPTURED_AT = Instant.parse("2026-11-01T12:00:00Z");
  private static final Instant BUCKET_START = Instant.parse("2026-11-01T00:00:00Z");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private PortfolioMarketDataService portfolioMarketDataService;

  @Test
  void latestReturnsServiceResponseAndDisablesCaching() throws Exception {
    String authorizationHeader = createAuthorizationHeader(TEST_USER_ID);
    AssetMarketPriceDto quote =
        new AssetMarketPriceDto(
            TEST_ASSET_ID,
            TEST_PORTFOLIO_ID,
            "MSFT",
            new BigDecimal("2.00000000"),
            new BigDecimal("400.0000"),
            new BigDecimal("425.50"),
            CAPTURED_AT);
    when(portfolioMarketDataService.getLatestPrices(
            TEST_USER_ID, List.of(TEST_PORTFOLIO_ID), null, authorizationHeader))
        .thenReturn(List.of(quote));

    mockMvc
        .perform(
            get(BASE_URL + "/latest")
                .param("portfolioIds", String.valueOf(TEST_PORTFOLIO_ID))
                .header(AUTHORIZATION_HEADER, authorizationHeader))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$[0].assetId").value(TEST_ASSET_ID))
        .andExpect(jsonPath("$[0].symbol").value("MSFT"))
        .andExpect(jsonPath("$[0].marketPrice").value(425.50))
        .andExpect(jsonPath("$[0].averagePrice").value(400.0));

    verify(portfolioMarketDataService)
        .getLatestPrices(TEST_USER_ID, List.of(TEST_PORTFOLIO_ID), null, authorizationHeader);
  }

  @Test
  void historyPassesSelectionAndRangeAndReturnsNoStoreResponse() throws Exception {
    String authorizationHeader = createAuthorizationHeader(TEST_USER_ID);
    Instant from = Instant.parse("2026-10-01T00:00:00Z");
    Instant to = Instant.parse("2026-11-01T23:59:59Z");
    AssetPriceCheckpointDto checkpoint =
        new AssetPriceCheckpointDto(
            TEST_ASSET_ID,
            TEST_PORTFOLIO_ID,
            "MSFT",
            new BigDecimal("425.50"),
            CAPTURED_AT,
            BUCKET_START);
    when(portfolioMarketDataService.getPriceHistory(
            TEST_USER_ID, null, List.of(TEST_ASSET_ID), from, to, authorizationHeader))
        .thenReturn(List.of(checkpoint));

    mockMvc
        .perform(
            get(BASE_URL + "/history")
                .param("assetIds", String.valueOf(TEST_ASSET_ID))
                .param("from", from.toString())
                .param("to", to.toString())
                .header(AUTHORIZATION_HEADER, authorizationHeader))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$[0].assetId").value(TEST_ASSET_ID))
        .andExpect(jsonPath("$[0].price").value(425.50))
        .andExpect(jsonPath("$[0].bucketStart").value(BUCKET_START.toString()));

    verify(portfolioMarketDataService)
        .getPriceHistory(TEST_USER_ID, null, List.of(TEST_ASSET_ID), from, to, authorizationHeader);
  }
}
