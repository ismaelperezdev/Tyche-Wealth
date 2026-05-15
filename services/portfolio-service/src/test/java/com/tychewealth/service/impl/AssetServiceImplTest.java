package com.tychewealth.service.impl;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_RESULT_KEY_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_FILE_PART_NAME;
import static com.tychewealth.constants.TestConstants.TEST_MISSING_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AssetTestData.AI_RESPONSE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_EXTRACTED_TEXT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static com.tychewealth.testdata.AssetTestData.validImportedAssetCandidate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.dto.asset.request.AssetImportPayloadDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ImportAssetsHelper;
import com.tychewealth.service.helper.asset.ai.AiResponseParser;
import com.tychewealth.service.helper.asset.ai.ImportAssetsAiHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

  @Mock private AssetValidationHelper assetValidationHelper;
  @Mock private ImportAssetsAiHelper importAssetsAiHelper;
  @Mock private ImportAssetsHelper importAssetsHelper;
  @Mock private AiResponseParser aiResponseParser;
  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private AssetServiceImpl assetService;

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
    verify(assetValidationHelper).validateImportRequest(TEST_USER_ID, file);
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

    doThrow(validationException)
        .when(assetValidationHelper)
        .validateImportRequest(TEST_USER_ID, file);

    AssetImportException thrown =
        assertThrows(
            AssetImportException.class, () -> assetService.importAssets(TEST_USER_ID, file));

    assertSame(validationException, thrown);
    verify(assetValidationHelper).validateImportRequest(TEST_USER_ID, file);
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
            importId, TEST_USER_ID, TEST_ASSET_FILE_NAME, java.time.Instant.now(), persistedResponse);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(redisKey)).thenReturn("{\"importId\":\"" + TEST_ASSET_IMPORT_ID + "\"}");
    when(
            objectMapper.readValue(
                "{\"importId\":\"" + TEST_ASSET_IMPORT_ID + "\"}", AssetPersistRedisDto.class))
        .thenReturn(persistedImport);

    AssetImportResponseDto result = assetService.retrieveImportedAssets(TEST_USER_ID, importId);

    assertSame(persistedResponse, result);
    verify(assetValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(assetValidationHelper).validateRetrievedImportExists(persistedImport);
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
    doThrow(notFoundException)
        .when(assetValidationHelper)
        .validateRetrievedImportExists(null);

    PortfolioException thrown =
        assertThrows(
            PortfolioException.class,
            () -> assetService.retrieveImportedAssets(TEST_OTHER_USER_ID, importId));

    assertSame(notFoundException, thrown);
    verify(assetValidationHelper).validateAuthenticatedUser(TEST_OTHER_USER_ID);
    verify(assetValidationHelper).validateRetrievedImportExists(null);
    verifyNoInteractions(objectMapper);
  }
}
