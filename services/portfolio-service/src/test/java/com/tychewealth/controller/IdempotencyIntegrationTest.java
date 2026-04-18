package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_LONG_TERM;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testhelper.ConcurrentTestHelper.executeCreate;
import static com.tychewealth.testhelper.ConcurrentTestHelper.executeImport;
import static com.tychewealth.testhelper.ConcurrentTestHelper.runConcurrently;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.IdempotencyIntegrationTestConfig;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.asset.ImportAssetsAiHelper;
import com.tychewealth.testhelper.ConcurrentTestHelper.IntegrationResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = IdempotencyIntegrationTestConfig.class)
@ContextConfiguration(initializers = IdempotencyIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class IdempotencyIntegrationTest {

  private static final byte[] IMPORT_FILE_BYTES = "ticker,quantity\nAAPL,10".getBytes(UTF_8);
  private static final String IMPORT_FILE_NAME = "positions.csv";
  private static final String IMPORT_ENDPOINT = "/tyche-wealth/portfolio-service/v1/assets/import";
  private static final String AI_RESPONSE = "[{\"name\":\"Apple Inc.\",\"symbol\":\"AAPL\"}]";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;

  @MockitoBean private ImportAssetsAiHelper importAssetsAiHelper;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();

    when(importAssetsAiHelper.promptFast(anyString())).thenReturn(AI_RESPONSE);
    when(importAssetsAiHelper.parseAiAssets("ticker,quantity\nAAPL,10", AI_RESPONSE))
        .thenReturn(
            List.of(
                new AssetImportCandidateDto(
                    "Apple Inc.",
                    "AAPL",
                    AssetTypeEnum.STOCK,
                    new BigDecimal("10"),
                    new BigDecimal("150.00"),
                    CurrencyCodeEnum.USD)));
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
        runConcurrently(
            () -> executeCreate(mockMvc, TEST_USER_ID, requestBody),
            () -> executeCreate(mockMvc, TEST_USER_ID, requestBody));

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

  @Test
  void importHandlesConcurrentDuplicateAttachmentsWithSameResponse() throws Exception {
    List<IntegrationResponse> responses =
        runConcurrently(
            () ->
                executeImport(
                    mockMvc,
                    TEST_USER_ID,
                    IMPORT_ENDPOINT,
                    IMPORT_FILE_NAME,
                    "text/csv",
                    IMPORT_FILE_BYTES),
            () ->
                executeImport(
                    mockMvc,
                    TEST_USER_ID,
                    IMPORT_ENDPOINT,
                    IMPORT_FILE_NAME,
                    "text/csv",
                    IMPORT_FILE_BYTES));

    assertEquals(2, responses.size());
    assertTrue(responses.stream().allMatch(response -> response.status() == 200));
    assertEquals(0L, assetRepository.count());

    JsonNode firstBody = objectMapper.readTree(responses.get(0).body());
    JsonNode secondBody = objectMapper.readTree(responses.get(1).body());

    assertEquals(firstBody, secondBody);
    assertEquals(1, firstBody.get("assets").size());
    assertEquals("Apple Inc.", firstBody.get("assets").get(0).get("name").asText());
    assertEquals("AAPL", firstBody.get("assets").get(0).get("symbol").asText());
  }
}
