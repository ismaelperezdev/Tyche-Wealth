package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_BY_ID_URL;
import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ASSET_BASE_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ASSET_BATCH_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.CommonConstants.DESCRIPTION;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID_PATH_SEGMENT;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_RESULT_KEY_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_ACTION_CREATE;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_ACTION_DISCARD;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_FIELD_ACTION;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_FIELD_ASSETS;
import static com.tychewealth.constants.TestConstants.TEST_BATCH_FIELD_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_FILE_PART_NAME;
import static com.tychewealth.constants.TestConstants.TEST_JSON_CODE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_JSON_TYPE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_MISSING_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AssetTestData.AI_RESPONSE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_EXTRACTED_TEXT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_AVERAGE_PRICE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_QUANTITY;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.createRequestWithNameAndSymbol;
import static com.tychewealth.testdata.AssetTestData.defaultAssetEntity;
import static com.tychewealth.testdata.AssetTestData.defaultPortfolioEntity;
import static com.tychewealth.testdata.AssetTestData.validCreateRequest;
import static com.tychewealth.testdata.AssetTestData.validImportedAssetCandidate;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.AssetIntegrationTestConfig;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.asset.ai.AiResponseParser;
import com.tychewealth.service.helper.asset.ai.ImportAssetsAiHelper;
import com.tychewealth.testhelper.TestRedisSupport.InMemoryRedisState;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = AssetIntegrationTestConfig.class)
@ContextConfiguration(initializers = AssetIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class AssetApiControllerIntegrationTest {

  private static final int ATTACHMENT_SIZE_LIMIT_BYTES = 3_145_728;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private InMemoryRedisState redisState;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;

  @MockitoBean private ImportAssetsAiHelper importAssetsAiHelper;
  @MockitoBean private AiResponseParser aiResponseParser;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();
    Set<String> persistedImportKeys = redisState.keys("asset-import:result:*");
    redisState.deleteAll(persistedImportKeys);

    when(importAssetsAiHelper.prompt(anyString(), eq(AiModelTypeEnum.FAST)))
        .thenReturn(AI_RESPONSE);
    when(aiResponseParser.parseAiAssets(TEST_ASSET_EXTRACTED_TEXT, AI_RESPONSE))
        .thenReturn(List.of(validImportedAssetCandidate()));
  }

  @Test
  void importReturnsOkWhenAttachmentIsValid() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME,
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes(UTF_8));

    MvcResult mvcResult =
        mockMvc
            .perform(
                multipart(ASSET_IMPORT_URL)
                    .file(file)
                    .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
            .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
            .andExpect(jsonPath("$.fileName").doesNotExist())
            .andExpect(jsonPath("$.extractedText").doesNotExist())
            .andExpect(jsonPath("$.aiResponse").doesNotExist())
            .andExpect(jsonPath("$.importId").isString())
            .andExpect(jsonPath("$.assets[0].name").value(TEST_ASSET_NAME_APPLE))
            .andExpect(jsonPath("$.assets[0].symbol").value(TEST_ASSET_SYMBOL_AAPL))
            .andExpect(jsonPath("$.assets[0].assetType").value(AssetTypeEnum.STOCK.name()))
            .andExpect(
                jsonPath("$.assets[0].quantity").value(TEST_ASSET_RESPONSE_QUANTITY.intValue()))
            .andExpect(
                jsonPath("$.assets[0].averagePrice")
                    .value(TEST_ASSET_RESPONSE_AVERAGE_PRICE.doubleValue()))
            .andExpect(jsonPath("$.assets[0].currency").value(CurrencyCodeEnum.USD.name()))
            .andReturn();

    JsonNode responseBody = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    String importId = responseBody.get("importId").asText();
    String redisKey = "asset-import:result:" + TEST_USER_ID + ":" + importId;
    String persistedImportJson = redisState.get(redisKey);
    AssetPersistRedisDto persistedImport =
        objectMapper.readValue(persistedImportJson, AssetPersistRedisDto.class);

    org.junit.jupiter.api.Assertions.assertEquals(importId, persistedImport.getImportId());
    org.junit.jupiter.api.Assertions.assertEquals(TEST_USER_ID, persistedImport.getUserId());
    org.junit.jupiter.api.Assertions.assertEquals(
        TEST_ASSET_FILE_NAME, persistedImport.getFileName());
    org.junit.jupiter.api.Assertions.assertEquals(
        TEST_ASSET_NAME_APPLE, persistedImport.getResult().getAssets().getFirst().getName());
  }

  @Test
  void createReturnsCreatedWhenRequestIsValid() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetCreateRequestDto request = validCreateRequest();

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BASE_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value(TEST_ASSET_NAME_APPLE))
        .andExpect(jsonPath("$.symbol").value(TEST_ASSET_SYMBOL_AAPL))
        .andExpect(jsonPath("$.assetType").value(AssetTypeEnum.STOCK.name()))
        .andExpect(jsonPath("$.quantity").value(TEST_ASSET_RESPONSE_QUANTITY.intValue()))
        .andExpect(
            jsonPath("$.averagePrice").value(TEST_ASSET_RESPONSE_AVERAGE_PRICE.doubleValue()))
        .andExpect(jsonPath("$.currency").value(CurrencyCodeEnum.USD.name()));
  }

  @Test
  void createReturnsConflictWhenAssetNameAlreadyExistsInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = defaultAssetEntity(portfolio);
    assetRepository.saveAndFlush(existingAsset);

    AssetCreateRequestDto request =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_MSFT);

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BASE_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_NAME_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.ASSET_NAME_CONFLICT.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString(TEST_ASSET_NAME_APPLE)));
  }

  @Test
  void createReturnsConflictWhenAssetSymbolAlreadyExistsInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = defaultAssetEntity(portfolio);
    assetRepository.saveAndFlush(existingAsset);

    AssetCreateRequestDto request =
        createRequestWithNameAndSymbol("Microsoft Corporation", TEST_ASSET_SYMBOL_AAPL);

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BASE_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_SYMBOL_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.ASSET_SYMBOL_CONFLICT.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString(TEST_ASSET_SYMBOL_AAPL)));
  }

  @Test
  void retrieveReturnsOkWhenAssetExistsInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = assetRepository.saveAndFlush(defaultAssetEntity(portfolio));

    mockMvc
        .perform(
            get(
                    PORTFOLIO_ASSET_BASE_URL + TEST_ASSET_ID_PATH_SEGMENT,
                    portfolio.getId(),
                    existingAsset.getId())
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").value(existingAsset.getId()))
        .andExpect(jsonPath("$.name").value(TEST_ASSET_NAME_APPLE))
        .andExpect(jsonPath("$.symbol").value(TEST_ASSET_SYMBOL_AAPL))
        .andExpect(jsonPath("$.assetType").value(AssetTypeEnum.STOCK.name()))
        .andExpect(jsonPath("$.quantity").value(TEST_ASSET_RESPONSE_QUANTITY.intValue()))
        .andExpect(jsonPath("$.averagePrice").value(existingAsset.getAveragePrice().doubleValue()))
        .andExpect(jsonPath("$.currency").value(CurrencyCodeEnum.USD.name()));
  }

  @Test
  void updateReturnsOkWhenRequestIsValid() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = assetRepository.saveAndFlush(defaultAssetEntity(portfolio));
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_MSFT,
            AssetTypeEnum.STOCK,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    mockMvc
        .perform(
            patch(
                    PORTFOLIO_ASSET_BASE_URL + TEST_ASSET_ID_PATH_SEGMENT,
                    portfolio.getId(),
                    existingAsset.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").value(existingAsset.getId()))
        .andExpect(jsonPath("$.name").value(TEST_ASSET_NAME_MICROSOFT))
        .andExpect(jsonPath("$.symbol").value(TEST_ASSET_SYMBOL_MSFT));
  }

  @Test
  void updateReturnsConflictWhenAssetNameAlreadyExistsInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = assetRepository.saveAndFlush(defaultAssetEntity(portfolio));
    AssetEntity anotherAsset = defaultAssetEntity(portfolio);
    anotherAsset.setName(TEST_ASSET_NAME_MICROSOFT);
    anotherAsset.setSymbol(TEST_ASSET_SYMBOL_MSFT);
    assetRepository.saveAndFlush(anotherAsset);

    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_AAPL,
            AssetTypeEnum.STOCK,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    mockMvc
        .perform(
            patch(
                    PORTFOLIO_ASSET_BASE_URL + TEST_ASSET_ID_PATH_SEGMENT,
                    portfolio.getId(),
                    existingAsset.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_NAME_CONFLICT.getCode()));
  }

  @Test
  void updateReturnsConflictWhenAssetSymbolAlreadyExistsInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = assetRepository.saveAndFlush(defaultAssetEntity(portfolio));
    AssetEntity anotherAsset = defaultAssetEntity(portfolio);
    anotherAsset.setName("Microsoft Corporation");
    anotherAsset.setSymbol(TEST_ASSET_SYMBOL_MSFT);
    assetRepository.saveAndFlush(anotherAsset);

    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_APPLE,
            TEST_ASSET_SYMBOL_MSFT,
            AssetTypeEnum.STOCK,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    mockMvc
        .perform(
            patch(
                    PORTFOLIO_ASSET_BASE_URL + TEST_ASSET_ID_PATH_SEGMENT,
                    portfolio.getId(),
                    existingAsset.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_SYMBOL_CONFLICT.getCode()));
  }

  @Test
  void updateReturnsNotFoundWhenPortfolioDoesNotBelongToAuthenticatedUser() throws Exception {
    PortfolioEntity portfolio = defaultPortfolioEntity();
    portfolio.setUserId(TEST_OTHER_USER_ID);
    portfolio = portfolioRepository.saveAndFlush(portfolio);
    AssetEntity existingAsset = assetRepository.saveAndFlush(defaultAssetEntity(portfolio));
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_MSFT,
            AssetTypeEnum.STOCK,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    mockMvc
        .perform(
            patch(
                    PORTFOLIO_ASSET_BASE_URL + TEST_ASSET_ID_PATH_SEGMENT,
                    portfolio.getId(),
                    existingAsset.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getType()));
  }

  @Test
  void updateReturnsNotFoundWhenAssetDoesNotExistInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_MSFT,
            AssetTypeEnum.STOCK,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    mockMvc
        .perform(
            patch(PORTFOLIO_ASSET_BASE_URL + TEST_ASSET_ID_PATH_SEGMENT, portfolio.getId(), 999999L)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_NOT_FOUND.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.ASSET_NOT_FOUND.getType()));
  }

  @Test
  void createBatchReturnsCreatedWhenRequestIsValid() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    String redisKey =
        TEST_ASSET_IMPORT_RESULT_KEY_PREFIX + TEST_USER_ID + ":" + TEST_ASSET_IMPORT_ID;
    redisState.set(redisKey, "{\"importId\":\"" + TEST_ASSET_IMPORT_ID + "\"}", null);

    Map<String, Object> request =
        Map.of(
            TEST_BATCH_FIELD_ACTION,
            TEST_BATCH_ACTION_CREATE,
            TEST_BATCH_FIELD_IMPORT_ID,
            TEST_ASSET_IMPORT_ID,
            TEST_BATCH_FIELD_ASSETS,
            List.of(
                validCreateRequest(),
                createRequestWithNameAndSymbol(TEST_ASSET_NAME_MICROSOFT, TEST_ASSET_SYMBOL_MSFT)));

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BATCH_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value(TEST_ASSET_NAME_APPLE))
        .andExpect(jsonPath("$[0].symbol").value(TEST_ASSET_SYMBOL_AAPL))
        .andExpect(jsonPath("$[1].name").value(TEST_ASSET_NAME_MICROSOFT))
        .andExpect(jsonPath("$[1].symbol").value(TEST_ASSET_SYMBOL_MSFT));

    org.junit.jupiter.api.Assertions.assertEquals(
        2, assetRepository.findByPortfolioId(portfolio.getId()).size());
    org.junit.jupiter.api.Assertions.assertNull(redisState.get(redisKey));
  }

  @Test
  void createBatchDiscardReturnsCreatedAndDeletesPersistedImport() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    String redisKey =
        TEST_ASSET_IMPORT_RESULT_KEY_PREFIX + TEST_USER_ID + ":" + TEST_ASSET_IMPORT_ID;
    redisState.set(redisKey, "{\"importId\":\"" + TEST_ASSET_IMPORT_ID + "\"}", null);

    Map<String, Object> request =
        Map.of(
            TEST_BATCH_FIELD_ACTION,
            TEST_BATCH_ACTION_DISCARD,
            TEST_BATCH_FIELD_IMPORT_ID,
            TEST_ASSET_IMPORT_ID,
            TEST_BATCH_FIELD_ASSETS,
            List.of());

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BATCH_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$", hasSize(0)));

    org.junit.jupiter.api.Assertions.assertEquals(
        0, assetRepository.findByPortfolioId(portfolio.getId()).size());
    org.junit.jupiter.api.Assertions.assertNull(redisState.get(redisKey));
  }

  @Test
  void createBatchReturnsConflictWhenAssetSymbolAlreadyExistsInPortfolio() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());
    AssetEntity existingAsset = defaultAssetEntity(portfolio);
    assetRepository.saveAndFlush(existingAsset);

    Map<String, Object> request =
        Map.of(
            TEST_BATCH_FIELD_ACTION,
            TEST_BATCH_ACTION_CREATE,
            TEST_BATCH_FIELD_ASSETS,
            List.of(
                createRequestWithNameAndSymbol(TEST_ASSET_NAME_MICROSOFT, TEST_ASSET_SYMBOL_AAPL)));

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BATCH_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_SYMBOL_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.ASSET_SYMBOL_CONFLICT.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString(TEST_ASSET_SYMBOL_AAPL)));
  }

  @Test
  void createBatchReturnsConflictWhenBatchContainsDuplicateNames() throws Exception {
    PortfolioEntity portfolio = portfolioRepository.saveAndFlush(defaultPortfolioEntity());

    Map<String, Object> request =
        Map.of(
            TEST_BATCH_FIELD_ACTION,
            TEST_BATCH_ACTION_CREATE,
            TEST_BATCH_FIELD_ASSETS,
            List.of(
                createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL),
                createRequestWithNameAndSymbol(
                    "  " + TEST_ASSET_NAME_APPLE + "  ", TEST_ASSET_SYMBOL_MSFT)));

    mockMvc
        .perform(
            post(PORTFOLIO_ASSET_BATCH_URL, portfolio.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_NAME_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.ASSET_NAME_CONFLICT.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString(TEST_ASSET_NAME_APPLE)));
  }

  @Test
  void importReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME,
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes(UTF_8));

    mockMvc
        .perform(multipart(ASSET_IMPORT_URL).file(file))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION).value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void importReturnsBadRequestWhenAttachmentIsEmpty() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME, TEST_ASSET_FILE_NAME, TEST_ASSET_CONTENT_TYPE_CSV, new byte[0]);

    mockMvc
        .perform(
            multipart(ASSET_IMPORT_URL)
                .file(file)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("file must not be empty")));
  }

  @Test
  void importReturnsBadRequestWhenAttachmentExceedsSizeLimit() throws Exception {
    byte[] oversizedContent = new byte[ATTACHMENT_SIZE_LIMIT_BYTES + 1];
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME,
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            oversizedContent);

    mockMvc
        .perform(
            multipart(ASSET_IMPORT_URL)
                .file(file)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH)
                .value(ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH)
                .value(ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED.getType()));
  }

  @Test
  void retrieveImportedAssetsReturnsOkWhenPersistedImportExists() throws Exception {
    AssetImportResponseDto response =
        new AssetImportResponseDto(TEST_ASSET_IMPORT_ID, List.of(validImportedAssetCandidate()));
    AssetPersistRedisDto persistedImport =
        new AssetPersistRedisDto(
            TEST_ASSET_IMPORT_ID, TEST_USER_ID, TEST_ASSET_FILE_NAME, Instant.now(), response);

    redisState.set(
        "asset-import:result:" + TEST_USER_ID + ":" + TEST_ASSET_IMPORT_ID,
        objectMapper.writeValueAsString(persistedImport),
        null);

    mockMvc
        .perform(
            get(ASSET_IMPORT_BY_ID_URL, TEST_ASSET_IMPORT_ID)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.importId").value(TEST_ASSET_IMPORT_ID))
        .andExpect(jsonPath("$.assets[0].name").value(TEST_ASSET_NAME_APPLE))
        .andExpect(jsonPath("$.assets[0].symbol").value(TEST_ASSET_SYMBOL_AAPL))
        .andExpect(jsonPath("$.assets[0].assetType").value(AssetTypeEnum.STOCK.name()))
        .andExpect(jsonPath("$.assets[0].quantity").value(TEST_ASSET_RESPONSE_QUANTITY.intValue()))
        .andExpect(
            jsonPath("$.assets[0].averagePrice")
                .value(TEST_ASSET_RESPONSE_AVERAGE_PRICE.doubleValue()))
        .andExpect(jsonPath("$.assets[0].currency").value(CurrencyCodeEnum.USD.name()));
  }

  @Test
  void retrieveImportedAssetsReturnsNotFoundWhenPersistedImportDoesNotExist() throws Exception {
    mockMvc
        .perform(
            get(ASSET_IMPORT_BY_ID_URL, TEST_MISSING_ASSET_IMPORT_ID)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.ASSET_IMPORT_NOT_FOUND.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.ASSET_IMPORT_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.ASSET_IMPORT_NOT_FOUND.getDescription()));
  }
}
