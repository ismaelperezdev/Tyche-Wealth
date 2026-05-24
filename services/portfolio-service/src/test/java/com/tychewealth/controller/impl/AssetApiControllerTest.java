package com.tychewealth.controller.impl;

import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_FILE_PART_NAME;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_EXTRACTED_TEXT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.createRequestWithNameAndSymbol;
import static com.tychewealth.testdata.AssetTestData.defaultAssetResponseDto;
import static com.tychewealth.testdata.AssetTestData.validImportedAssetCandidate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.service.AssetService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssetApiControllerTest {

  @Mock private AssetService assetService;

  @InjectMocks private AssetApiController assetApiController;

  @Test
  void createReturnsCreatedResponse() {
    AssetCreateRequestDto request =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    AssetResponseDto response = defaultAssetResponseDto(100L);

    when(assetService.create(TEST_USER_ID, TEST_PORTFOLIO_ID, request)).thenReturn(response);

    ResponseEntity<AssetResponseDto> result =
        assetApiController.create(TEST_USER_ID, TEST_PORTFOLIO_ID, request);

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(assetService).create(TEST_USER_ID, TEST_PORTFOLIO_ID, request);
  }

  @Test
  void importAssetsReturnsOkResponse() {
    MockMultipartFile file =
        new MockMultipartFile(
            TEST_FILE_PART_NAME,
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes(StandardCharsets.UTF_8));
    AssetImportResponseDto response =
        new AssetImportResponseDto(TEST_ASSET_IMPORT_ID, List.of(validImportedAssetCandidate()));

    when(assetService.importAssets(TEST_USER_ID, file)).thenReturn(response);

    ResponseEntity<AssetImportResponseDto> result =
        assetApiController.importAssets(TEST_USER_ID, file);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(assetService).importAssets(TEST_USER_ID, file);
  }

  @Test
  void retrieveReturnsOkResponse() {
    Long assetId = 100L;
    AssetResponseDto response = defaultAssetResponseDto(assetId);

    when(assetService.retrieve(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId)).thenReturn(response);

    ResponseEntity<AssetResponseDto> result =
        assetApiController.retrieve(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(assetService).retrieve(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId);
  }

  @Test
  void updateReturnsOkResponse() {
    Long assetId = 100L;
    AssetUpdateRequestDto request =
        new AssetUpdateRequestDto(
            TEST_ASSET_NAME_MICROSOFT,
            TEST_ASSET_SYMBOL_MSFT,
            AssetTypeEnum.STOCK,
            new BigDecimal("12.00000000"),
            new BigDecimal("175.5000"),
            CurrencyCodeEnum.USD);
    AssetResponseDto response = defaultAssetResponseDto(assetId);

    when(assetService.update(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId, request))
        .thenReturn(response);

    ResponseEntity<AssetResponseDto> result =
        assetApiController.update(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId, request);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(assetService).update(TEST_USER_ID, TEST_PORTFOLIO_ID, assetId, request);
  }

  @Test
  void retrieveImportedAssetsReturnsOkResponse() {
    String importId = TEST_ASSET_IMPORT_ID;
    AssetImportResponseDto response =
        new AssetImportResponseDto(importId, List.of(validImportedAssetCandidate()));

    when(assetService.retrieveImportedAssets(TEST_USER_ID, importId)).thenReturn(response);

    ResponseEntity<AssetImportResponseDto> result =
        assetApiController.retrieveImportedAssets(TEST_USER_ID, importId);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(assetService).retrieveImportedAssets(TEST_USER_ID, importId);
  }
}
