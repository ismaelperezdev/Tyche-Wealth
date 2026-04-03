package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_LONG_TERM;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testhelper.ConcurrentTestHelper.runConcurrently;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.PortfolioIntegrationTestConfig;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = PortfolioIntegrationTestConfig.class)
@ContextConfiguration(initializers = PortfolioIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class PortfolioConcurrencyIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();
  }

  @Test
  void createHandlesConcurrentDuplicateRequestsWithSingleInsert() throws Exception {
    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    List<IntegrationResponse> responses =
        runConcurrently(() -> executeCreate(requestBody), () -> executeCreate(requestBody));

    long createdCount = responses.stream().filter(response -> response.status() == 201).count();
    long conflictCount = responses.stream().filter(response -> response.status() == 409).count();

    assertEquals(1, createdCount);
    assertEquals(1, conflictCount);
    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID).size());
    assertTrue(
        responses.stream()
            .filter(response -> response.status() == 409)
            .map(IntegrationResponse::body)
            .anyMatch(body -> body.contains("PORTFOLIO_NAME_CONFLICT")));
  }

  private IntegrationResponse executeCreate(String requestBody) throws Exception {
    MvcResult result =
        createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody).andReturn();
    return new IntegrationResponse(
        result.getResponse().getStatus(), result.getResponse().getContentAsString());
  }

  private record IntegrationResponse(int status, String body) {}
}
