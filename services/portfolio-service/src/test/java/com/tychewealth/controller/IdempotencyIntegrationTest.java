package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_ACTION_CREATE;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_FIELD_ACTION;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_FIELD_ASSETS;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_LONG_TERM;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_EXTRACTED_TEXT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.createRequestWithNameAndSymbol;
import static com.tychewealth.testdata.AssetTestData.defaultPortfolioEntity;
import static com.tychewealth.testhelper.ConcurrentTestHelper.executeAssetBatchCreate;
import static com.tychewealth.testhelper.ConcurrentTestHelper.executeAssetCreate;
import static com.tychewealth.testhelper.ConcurrentTestHelper.executeCreate;
import static com.tychewealth.testhelper.ConcurrentTestHelper.executeImport;
import static com.tychewealth.testhelper.ConcurrentTestHelper.runConcurrently;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.IdempotencyIntegrationTestConfig;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.asset.ai.AiResponseParser;
import com.tychewealth.service.helper.asset.ai.ImportAssetsAiHelper;
import com.tychewealth.testhelper.ConcurrentTestHelper.IntegrationResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

  private static final byte[] IMPORT_FILE_BYTES = TEST_ASSET_EXTRACTED_TEXT.getBytes(UTF_8);
  private static final String IMPORT_ENDPOINT = "/tyche-wealth/portfolio-service/v1/assets/import";
  private static final String AI_RESPONSE = "[{\"name\":\"Apple Inc.\",\"symbol\":\"AAPL\"}]";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;

  @MockitoBean private ImportAssetsAiHelper importAssetsAiHelper;
  @MockitoBean private AiResponseParser aiResponseParser;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();

    when(importAssetsAiHelper.prompt(anyString(), eq(AiModelTypeEnum.FAST)))
        .thenReturn(AI_RESPONSE);
    when(aiResponseParser.parseAiAssets(TEST_ASSET_EXTRACTED_TEXT, AI_RESPONSE))
        .thenReturn(
            List.of(
                new AssetImportCandidateDto(
                    TEST_ASSET_NAME_APPLE,
                    TEST_ASSET_SYMBOL_AAPL,
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
            .anyMatch(body -> body.contains(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getType())));
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
                    TEST_ASSET_FILE_NAME,
                    TEST_ASSET_CONTENT_TYPE_CSV,
                    IMPORT_FILE_BYTES),
            () ->
                executeImport(
                    mockMvc,
                    TEST_USER_ID,
                    IMPORT_ENDPOINT,
                    TEST_ASSET_FILE_NAME,
                    TEST_ASSET_CONTENT_TYPE_CSV,
                    IMPORT_FILE_BYTES));

    assertEquals(2, responses.size());
    assertTrue(responses.stream().allMatch(response -> response.status() == 200));
    assertEquals(0L, assetRepository.count());

    JsonNode firstBody = objectMapper.readTree(responses.get(0).body());
    JsonNode secondBody = objectMapper.readTree(responses.get(1).body());

    assertEquals(firstBody, secondBody);
    assertEquals(1, firstBody.get(TEST_BATCH_FIELD_ASSETS).size());
    assertEquals(
        TEST_ASSET_NAME_APPLE, firstBody.get(TEST_BATCH_FIELD_ASSETS).get(0).get("name").asText());
    assertEquals(
        TEST_ASSET_SYMBOL_AAPL,
        firstBody.get(TEST_BATCH_FIELD_ASSETS).get(0).get("symbol").asText());
  }

  @Test
  void assetCreateHandlesConcurrentDuplicateRequestsWithSingleInsert() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetCreateRequestDto request =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    String requestBody = objectMapper.writeValueAsString(request);

    List<IntegrationResponse> responses =
        runConcurrently(
            () -> executeAssetCreate(mockMvc, TEST_USER_ID, portfolio.getId(), requestBody),
            () -> executeAssetCreate(mockMvc, TEST_USER_ID, portfolio.getId(), requestBody));

    long createdCount = responses.stream().filter(response -> response.status() == 201).count();
    long conflictCount = responses.stream().filter(response -> response.status() == 409).count();

    assertEquals(1, createdCount);
    assertEquals(1, conflictCount);
    assertEquals(1, assetRepository.findByPortfolioId(portfolio.getId()).size());
  }

  @Test
  void assetBatchCreateHandlesConcurrentDuplicateRequestsWithSingleInsert() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    String requestBody =
        objectMapper.writeValueAsString(
            Map.of(
                TEST_BATCH_FIELD_ACTION,
                TEST_BATCH_ACTION_CREATE,
                TEST_BATCH_FIELD_ASSETS,
                List.of(
                    createRequestWithNameAndSymbol(
                        TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL))));

    List<IntegrationResponse> responses =
        runConcurrently(
            () -> executeAssetBatchCreate(mockMvc, TEST_USER_ID, portfolio.getId(), requestBody),
            () -> executeAssetBatchCreate(mockMvc, TEST_USER_ID, portfolio.getId(), requestBody));

    long createdCount = responses.stream().filter(response -> response.status() == 201).count();
    long conflictCount = responses.stream().filter(response -> response.status() == 409).count();

    assertEquals(1, createdCount);
    assertEquals(1, conflictCount);
    assertEquals(1, assetRepository.findByPortfolioId(portfolio.getId()).size());
  }
}
