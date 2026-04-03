package com.tychewealth.service.impl;

import static com.tychewealth.constants.CommonConstants.ERROR;
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

import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ImportAssetsAiHelper;
import com.tychewealth.service.helper.asset.ImportAssetsHelper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

  @Mock private AssetValidationHelper assetValidationHelper;
  @Mock private ImportAssetsAiHelper importAssetsAiHelper;
  @Mock private ImportAssetsHelper importAssetsHelper;

  @InjectMocks private AssetServiceImpl assetService;

  @Test
  void importAssetsBuildsPromptDelegatesToAiAndReturnsEnrichedResponse() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes());
    AssetImportResponseDto payload =
        new AssetImportResponseDto(TEST_ASSET_FILE_NAME, TEST_ASSET_EXTRACTED_TEXT, null, null);
    List<AssetImportCandidateDto> parsedAssets = List.of(validImportedAssetCandidate());

    when(importAssetsHelper.buildImportPayload(file)).thenReturn(payload);
    when(importAssetsAiHelper.promptFast(
            org.mockito.ArgumentMatchers.contains(TEST_ASSET_EXTRACTED_TEXT)))
        .thenReturn(AI_RESPONSE);
    when(importAssetsAiHelper.parseAiAssets(AI_RESPONSE)).thenReturn(parsedAssets);

    AssetImportResponseDto result = assetService.importAssets(TEST_USER_ID, file);

    assertSame(payload, result);
    assertEquals(AI_RESPONSE, result.getAiResponse());
    assertEquals(parsedAssets, result.getAssets());
    verify(assetValidationHelper).validateImportRequest(TEST_USER_ID, file);
    verify(importAssetsHelper).buildImportPayload(file);
    verify(importAssetsAiHelper)
        .promptFast(org.mockito.ArgumentMatchers.contains(TEST_ASSET_EXTRACTED_TEXT));
    verify(importAssetsAiHelper).parseAiAssets(AI_RESPONSE);
  }

  @Test
  void importAssetsStopsWhenValidationFails() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", TEST_ASSET_FILE_NAME, TEST_ASSET_CONTENT_TYPE_CSV, new byte[0]);
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
    verify(importAssetsAiHelper, never()).promptFast(org.mockito.ArgumentMatchers.anyString());
    verify(importAssetsAiHelper, never()).parseAiAssets(org.mockito.ArgumentMatchers.anyString());
  }
}
