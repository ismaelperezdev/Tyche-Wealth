package com.tychewealth.service.impl;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.DELETE_ACTION;
import static com.tychewealth.constants.LogConstants.LIST_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.LogConstants.UPDATE_ACTION;
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_RESULT_KEY_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_FILE_PART_NAME;
import static com.tychewealth.constants.TestConstants.TEST_MISSING_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AssetTestData.AI_RESPONSE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_EXTRACTED_TEXT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.validImportedAssetCandidate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetBatchCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetImportPayloadDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetBatchActionEnum;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.asset.AssetMapper;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.service.helper.CommonValidationHelper;
import com.tychewealth.service.helper.asset.AssetCreateHelper;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ImportAssetsHelper;
import com.tychewealth.service.helper.asset.ai.AiResponseParser;
import com.tychewealth.service.helper.asset.ai.AssetAiValidationHelper;
import com.tychewealth.service.helper.asset.ai.ImportAssetsAiHelper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

  @Mock private AssetRepository assetRepository;
  @Mock private AssetCreateHelper assetCreateHelper;
  @Mock private AssetMapper assetMapper;
  @Mock private AssetValidationHelper assetValidationHelper;
  @Mock private AssetAiValidationHelper assetAiValidationHelper;
  @Mock private CommonValidationHelper commonValidationHelper;
  @Mock private ImportAssetsAiHelper importAssetsAiHelper;
  @Mock private ImportAssetsHelper importAssetsHelper;
  @Mock private AiResponseParser aiResponseParser;
  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private AssetServiceImpl assetService;
  private PortfolioEntity portfolio;

  @BeforeEach
  void setUp() {
    portfolio = new PortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);
  }

  @Test
  void createValidatesAndDelegatesToCreateHelper() {
    AssetCreateRequestDto request = new AssetCreateRequestDto();
    request.setName(TEST_ASSET_NAME_APPLE);
    request.setSymbol(TEST_ASSET_SYMBOL_AAPL);
    AssetResponseDto response = new AssetResponseDto();
    response.setId(20L);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION))
        .thenReturn(portfolio);
    when(assetCreateHelper.create(portfolio, request)).thenReturn(response);

    AssetResponseDto result = assetService.create(TEST_USER_ID, TEST_PORTFOLIO_ID, request);

    assertSame(response, result);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION);
    verify(assetValidationHelper).validateCreateLimit(TEST_PORTFOLIO_ID);
    verify(assetValidationHelper).validateCreateNameConflict(TEST_PORTFOLIO_ID, request.getName());
    verify(assetValidationHelper)
        .validateCreateSymbolConflict(TEST_PORTFOLIO_ID, request.getSymbol());
    verify(assetCreateHelper).create(portfolio, request);
  }

  @Test
  void createStopsWhenNameValidationFails() {
    AssetCreateRequestDto request = new AssetCreateRequestDto();
    request.setName(TEST_ASSET_NAME_APPLE);
    PortfolioException conflict =
        new PortfolioException(
            ErrorDefinition.ASSET_NAME_CONFLICT, java.util.Map.of(), HttpStatus.CONFLICT);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION))
        .thenReturn(portfolio);
    doThrow(conflict)
        .when(assetValidationHelper)
        .validateCreateNameConflict(TEST_PORTFOLIO_ID, request.getName());

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () -> assetService.create(TEST_USER_ID, TEST_PORTFOLIO_ID, request));

    assertSame(conflict, thrown);
    verify(assetValidationHelper).validateCreateLimit(TEST_PORTFOLIO_ID);
    verify(assetValidationHelper).validateCreateNameConflict(TEST_PORTFOLIO_ID, request.getName());
    verifyNoInteractions(assetCreateHelper);
  }

  @Test
  void retrieveValidatesAndReturnsMappedAsset() {
    Long assetId = TEST_ASSET_ID;
    AssetEntity asset = new AssetEntity();
    asset.setId(assetId);
    AssetResponseDto response = new AssetResponseDto();
    response.setId(assetId);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION))
        .thenReturn(portfolio);
    when(assetValidationHelper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId))
        .thenReturn(asset);
    when(assetMapper.toDto(asset)).thenReturn(response);

    AssetResponseDto result = assetService.retrieve(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId);

    assertSame(response, result);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION);
    verify(assetValidationHelper).validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId);
    verify(assetMapper).toDto(asset);
  }

  @Test
  void retrieveStopsWhenAssetDoesNotExist() {
    Long assetId = TEST_ASSET_ID;
    PortfolioException notFound =
        new PortfolioException(
            ErrorDefinition.ASSET_NOT_FOUND, java.util.Map.of(), HttpStatus.NOT_FOUND);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION))
        .thenReturn(portfolio);
    when(assetValidationHelper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId))
        .thenThrow(notFound);

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () -> assetService.retrieve(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId));

    assertSame(notFound, thrown);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION);
    verify(assetValidationHelper).validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId);
    verifyNoInteractions(assetMapper);
  }

  @Test
  void listAssetsValidatesOwnershipAndReturnsMappedAssets() {
    AssetEntity first = new AssetEntity();
    first.setId(1L);
    AssetEntity second = new AssetEntity();
    second.setId(2L);
    AssetResponseDto firstDto = new AssetResponseDto();
    firstDto.setId(1L);
    AssetResponseDto secondDto = new AssetResponseDto();
    secondDto.setId(2L);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, LIST_ASSETS_ACTION))
        .thenReturn(portfolio);
    when(assetRepository.findByPortfolioId(eq(TEST_PORTFOLIO_ID), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(first, second)));
    when(assetMapper.toDto(first)).thenReturn(firstDto);
    when(assetMapper.toDto(second)).thenReturn(secondDto);

    Page<AssetResponseDto> result = assetService.listAssets(TEST_USER_ID, TEST_PORTFOLIO_ID, 0, 10);

    assertEquals(2, result.getContent().size());
    assertSame(firstDto, result.getContent().get(0));
    assertSame(secondDto, result.getContent().get(1));
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, LIST_ASSETS_ACTION);
    verify(assetRepository).findByPortfolioId(eq(TEST_PORTFOLIO_ID), any(Pageable.class));
  }

  @Test
  void deleteValidatesOwnershipAndDelegatesToRepository() {
    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, DELETE_ACTION))
        .thenReturn(portfolio);

    assetService.delete(TEST_USER_ID, TEST_PORTFOLIO_ID, TEST_ASSET_ID);

    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, DELETE_ACTION);
    verify(assetRepository).deleteByIdAndPortfolioId(TEST_ASSET_ID, TEST_PORTFOLIO_ID);
  }

  @Test
  void deleteStopsWhenPortfolioValidationFails() {
    PortfolioException notFound =
        new PortfolioException(
            ErrorDefinition.PORTFOLIO_NOT_FOUND, java.util.Map.of(), HttpStatus.NOT_FOUND);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, DELETE_ACTION))
        .thenThrow(notFound);

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () -> assetService.delete(TEST_USER_ID, TEST_PORTFOLIO_ID, TEST_ASSET_ID));

    assertSame(notFound, thrown);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, DELETE_ACTION);
    verifyNoInteractions(assetRepository);
  }

  @Test
  void updateValidatesConflictsAndPersistsWhenNameAndSymbolChange() {
    Long assetId = TEST_ASSET_ID;
    AssetEntity asset = new AssetEntity();
    asset.setId(assetId);
    asset.setName(TEST_ASSET_NAME_APPLE);
    asset.setSymbol(TEST_ASSET_SYMBOL_AAPL);
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_MSFT,
            AssetTypeEnum.STOCK,
            new BigDecimal("10.00000000"),
            new BigDecimal("150.0000"),
            CurrencyCodeEnum.USD);
    AssetResponseDto response = new AssetResponseDto();
    response.setId(assetId);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, UPDATE_ACTION))
        .thenReturn(portfolio);
    when(assetValidationHelper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId))
        .thenReturn(asset);
    when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
    when(assetMapper.toDto(asset)).thenReturn(response);

    AssetResponseDto result =
        assetService.update(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId, request);

    assertSame(response, result);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, UPDATE_ACTION);
    verify(assetValidationHelper).validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId);
    verify(assetValidationHelper)
        .validateCreateNameConflict(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_MICROSOFT);
    verify(assetValidationHelper)
        .validateCreateSymbolConflict(TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_MSFT);
    verify(assetMapper).update(request, asset);
    verify(assetRepository).saveAndFlush(asset);
    verify(assetMapper).toDto(asset);
  }

  @Test
  void updateSkipsConflictChecksWhenNameAndSymbolDoNotChange() {
    Long assetId = TEST_ASSET_ID;
    AssetEntity asset = new AssetEntity();
    asset.setId(assetId);
    asset.setName(TEST_ASSET_NAME_APPLE);
    asset.setSymbol(TEST_ASSET_SYMBOL_AAPL);
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_APPLE,
            TEST_ASSET_SYMBOL_AAPL,
            AssetTypeEnum.STOCK,
            new BigDecimal("10.00000000"),
            new BigDecimal("150.0000"),
            CurrencyCodeEnum.USD);
    AssetResponseDto response = new AssetResponseDto();
    response.setId(assetId);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, UPDATE_ACTION))
        .thenReturn(portfolio);
    when(assetValidationHelper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId))
        .thenReturn(asset);
    when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
    when(assetMapper.toDto(asset)).thenReturn(response);

    assetService.update(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId, request);

    verify(assetValidationHelper, never()).validateCreateNameConflict(anyLong(), anyString());
    verify(assetValidationHelper, never()).validateCreateSymbolConflict(anyLong(), anyString());
    verify(assetMapper).update(request, asset);
    verify(assetRepository).saveAndFlush(asset);
  }

  @Test
  void updateStopsWhenNameConflictValidationFails() {
    Long assetId = TEST_ASSET_ID;
    AssetEntity asset = new AssetEntity();
    asset.setId(assetId);
    asset.setName(TEST_ASSET_NAME_APPLE);
    asset.setSymbol(TEST_ASSET_SYMBOL_AAPL);
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_AAPL,
            AssetTypeEnum.STOCK,
            new BigDecimal("10.00000000"),
            new BigDecimal("150.0000"),
            CurrencyCodeEnum.USD);
    PortfolioException conflict =
        new PortfolioException(
            ErrorDefinition.ASSET_NAME_CONFLICT, java.util.Map.of(), HttpStatus.CONFLICT);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, UPDATE_ACTION))
        .thenReturn(portfolio);
    when(assetValidationHelper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId))
        .thenReturn(asset);
    doThrow(conflict)
        .when(assetValidationHelper)
        .validateCreateNameConflict(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_MICROSOFT);

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () -> assetService.update(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId, request));

    assertSame(conflict, thrown);
    verify(assetMapper, never()).update(any(), any());
    verifyNoInteractions(assetRepository);
  }

  @Test
  void createBatchFromImportedAssetsForCreateValidatesAndDelegatesToCreateHelper() {
    AssetCreateRequestDto first = new AssetCreateRequestDto();
    first.setName(TEST_ASSET_NAME_APPLE);
    first.setSymbol(TEST_ASSET_SYMBOL_AAPL);
    AssetCreateRequestDto second = new AssetCreateRequestDto();
    second.setName(TEST_ASSET_NAME_MICROSOFT);
    second.setSymbol(TEST_ASSET_SYMBOL_MSFT);
    AssetBatchCreateRequestDto request =
        new AssetBatchCreateRequestDto(
            AssetBatchActionEnum.CREATE, TEST_ASSET_IMPORT_ID, List.of(first, second));
    List<AssetResponseDto> created = List.of(new AssetResponseDto(), new AssetResponseDto());

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION))
        .thenReturn(portfolio);
    when(assetCreateHelper.createBatch(portfolio, List.of(first, second))).thenReturn(created);

    List<AssetResponseDto> result =
        assetService.createBatchFromImportedAssets(TEST_USER_ID, TEST_PORTFOLIO_ID, request);

    assertSame(created, result);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION);
    verify(assetValidationHelper)
        .validateBatchCreateRequest(
            AssetBatchActionEnum.CREATE, TEST_ASSET_IMPORT_ID, List.of(first, second));
    verify(assetValidationHelper).validateBatchCreateLimit(TEST_PORTFOLIO_ID, 2);
    verify(assetValidationHelper).validateBatchCreateRequestDuplicates(List.of(first, second));
    verify(assetValidationHelper)
        .validateBatchCreateDatabaseConflicts(TEST_PORTFOLIO_ID, List.of(first, second));
    verify(assetCreateHelper).createBatch(portfolio, List.of(first, second));
    verify(redisTemplate)
        .delete(ASSET_IMPORT_RESULT_KEY_PREFIX + TEST_USER_ID + ":" + TEST_ASSET_IMPORT_ID);
  }

  @Test
  void createBatchFromImportedAssetsForDiscardReturnsEmptyAndDeletesImport() {
    AssetBatchCreateRequestDto request =
        new AssetBatchCreateRequestDto(
            AssetBatchActionEnum.DISCARD, TEST_ASSET_IMPORT_ID, List.of());

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION))
        .thenReturn(portfolio);

    List<AssetResponseDto> result =
        assetService.createBatchFromImportedAssets(TEST_USER_ID, TEST_PORTFOLIO_ID, request);

    assertEquals(0, result.size());
    verify(assetValidationHelper)
        .validateBatchCreateRequest(AssetBatchActionEnum.DISCARD, TEST_ASSET_IMPORT_ID, List.of());
    verify(redisTemplate)
        .delete(ASSET_IMPORT_RESULT_KEY_PREFIX + TEST_USER_ID + ":" + TEST_ASSET_IMPORT_ID);
    verify(assetCreateHelper, never()).createBatch(any(), any());
  }

  @Test
  void createBatchFromImportedAssetsTranslatesDataIntegrityViolationException() {
    AssetCreateRequestDto first = new AssetCreateRequestDto();
    first.setName(TEST_ASSET_NAME_APPLE);
    first.setSymbol(TEST_ASSET_SYMBOL_AAPL);
    AssetBatchCreateRequestDto request =
        new AssetBatchCreateRequestDto(
            AssetBatchActionEnum.CREATE, TEST_ASSET_IMPORT_ID, List.of(first));
    DataIntegrityViolationException dataException =
        new DataIntegrityViolationException("duplicate symbol");
    PortfolioException translated =
        new PortfolioException(
            ErrorDefinition.ASSET_SYMBOL_CONFLICT, java.util.Map.of(), HttpStatus.CONFLICT);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, CREATE_ACTION))
        .thenReturn(portfolio);
    when(assetCreateHelper.createBatch(portfolio, List.of(first))).thenThrow(dataException);
    when(assetValidationHelper.translateSymbolPersistenceConflict(
            dataException, TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_AAPL))
        .thenReturn(translated);

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () ->
                assetService.createBatchFromImportedAssets(
                    TEST_USER_ID, TEST_PORTFOLIO_ID, request));

    assertSame(translated, thrown);
    verify(assetValidationHelper)
        .translateSymbolPersistenceConflict(
            dataException, TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_AAPL);
  }

  @Test
  void importAssetsBuildsPromptDelegatesToAiAndReturnsEnrichedResponse() {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME,
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes());
    AssetImportPayloadDto payload =
        new AssetImportPayloadDto(TEST_ASSET_FILE_NAME, TEST_ASSET_EXTRACTED_TEXT);
    List<AssetImportCandidateDto> parsedAssets = List.of(validImportedAssetCandidate());

    when(importAssetsHelper.buildImportPayload(file)).thenReturn(payload);
    when(importAssetsAiHelper.prompt(
            org.mockito.ArgumentMatchers.contains(TEST_ASSET_EXTRACTED_TEXT),
            eq(AiModelTypeEnum.FAST)))
        .thenReturn(AI_RESPONSE);
    when(aiResponseParser.parseAiAssets(TEST_ASSET_EXTRACTED_TEXT, AI_RESPONSE))
        .thenReturn(parsedAssets);

    AssetImportResponseDto result = assetService.importAssets(TEST_USER_ID, file);
    ArgumentCaptor<AssetPersistRedisDto> persistedImportCaptor =
        ArgumentCaptor.forClass(AssetPersistRedisDto.class);

    assertEquals(parsedAssets, result.getAssets());
    verify(importAssetsHelper).savePersistedImportResult(persistedImportCaptor.capture());
    assertEquals(persistedImportCaptor.getValue().getImportId(), result.getImportId());
    assertEquals(TEST_USER_ID, persistedImportCaptor.getValue().getUserId());
    assertEquals(TEST_ASSET_FILE_NAME, persistedImportCaptor.getValue().getFileName());
    assertEquals(result, persistedImportCaptor.getValue().getResult());
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(assetAiValidationHelper).validateImportRequest(file);
    verify(importAssetsHelper).buildImportPayload(file);
    verify(importAssetsAiHelper)
        .prompt(
            org.mockito.ArgumentMatchers.contains(TEST_ASSET_EXTRACTED_TEXT),
            eq(AiModelTypeEnum.FAST));
    verify(aiResponseParser).parseAiAssets(TEST_ASSET_EXTRACTED_TEXT, AI_RESPONSE);
  }

  @Test
  void importAssetsStopsWhenValidationFails() {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME, TEST_ASSET_FILE_NAME, TEST_ASSET_CONTENT_TYPE_CSV, new byte[0]);
    AssetImportException validationException =
        new AssetImportException(
            ErrorDefinition.GENERIC_BAD_REQUEST,
            java.util.Map.of(ERROR, "file must not be empty"),
            HttpStatus.BAD_REQUEST);

    doThrow(validationException).when(assetAiValidationHelper).validateImportRequest(file);

    AssetImportException thrown =
        assertThrows(
            AssetImportException.class, () -> assetService.importAssets(TEST_USER_ID, file));

    assertSame(validationException, thrown);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(assetAiValidationHelper).validateImportRequest(file);
    verify(importAssetsHelper, never()).buildImportPayload(file);
    verify(importAssetsAiHelper, never())
        .prompt(org.mockito.ArgumentMatchers.anyString(), any(AiModelTypeEnum.class));
    verify(aiResponseParser, never())
        .parseAiAssets(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void retrieveImportedAssetsReturnsPersistedResponseWhenImportExists() throws Exception {
    String importId = TEST_ASSET_IMPORT_ID;
    String redisKey = ASSET_IMPORT_RESULT_KEY_PREFIX + TEST_USER_ID + ":" + importId;
    AssetImportResponseDto persistedResponse =
        new AssetImportResponseDto(importId, List.of(validImportedAssetCandidate()));
    AssetPersistRedisDto persistedImport =
        new AssetPersistRedisDto(
            importId,
            TEST_USER_ID,
            TEST_ASSET_FILE_NAME,
            java.time.Instant.now(),
            persistedResponse);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(redisKey))
        .thenReturn("{\"importId\":\"" + TEST_ASSET_IMPORT_ID + "\"}");
    when(objectMapper.readValue(
            "{\"importId\":\"" + TEST_ASSET_IMPORT_ID + "\"}", AssetPersistRedisDto.class))
        .thenReturn(persistedImport);

    AssetImportResponseDto result = assetService.retrieveImportedAssets(TEST_USER_ID, importId);

    assertSame(persistedResponse, result);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(assetAiValidationHelper).validateRetrievedImportExists(persistedImport);
  }

  @Test
  void retrieveImportedAssetsReturnsNotFoundWhenImportDoesNotExist() {
    String importId = TEST_MISSING_ASSET_IMPORT_ID;
    String redisKey = ASSET_IMPORT_RESULT_KEY_PREFIX + TEST_OTHER_USER_ID + ":" + importId;
    PortfolioException notFoundException =
        new PortfolioException(
            ErrorDefinition.ASSET_IMPORT_NOT_FOUND, java.util.Map.of(), HttpStatus.NOT_FOUND);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(redisKey)).thenReturn(null);
    doThrow(notFoundException).when(assetAiValidationHelper).validateRetrievedImportExists(null);

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () -> assetService.retrieveImportedAssets(TEST_OTHER_USER_ID, importId));

    assertSame(notFoundException, thrown);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_OTHER_USER_ID);
    verify(assetAiValidationHelper).validateRetrievedImportExists(null);
    verifyNoInteractions(objectMapper);
  }
}
