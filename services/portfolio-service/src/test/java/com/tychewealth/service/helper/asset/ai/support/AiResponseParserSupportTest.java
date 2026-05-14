package com.tychewealth.service.helper.asset.ai.support;

import static com.tychewealth.testdata.AiTestData.TEST_ASSET_ISIN_APPLE;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AiTestData.TEST_BLANK_VALUE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_AVERAGE_PRICE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_QUANTITY;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.validImportedAssetCandidate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import org.junit.jupiter.api.Test;

class AiResponseParserSupportTest {

  private final AiResponseParserSupport support = new AiResponseParserSupport(new ObjectMapper());

  @Test
  void looksLikeIsinReturnsTrueForValidIsin() {
    assertTrue(support.looksLikeIsin(TEST_ASSET_ISIN_APPLE));
  }

  @Test
  void looksLikeIsinReturnsFalseForTicker() {
    assertFalse(support.looksLikeIsin(TEST_ASSET_SYMBOL_AAPL));
  }

  @Test
  void firstNonBlankReturnsPrimaryWhenPresent() {
    assertEquals(
        TEST_ASSET_NAME_APPLE,
        support.firstNonBlank(TEST_ASSET_NAME_APPLE, TEST_ASSET_NAME_MICROSOFT));
  }

  @Test
  void firstNonBlankReturnsFallbackWhenPrimaryIsBlank() {
    assertEquals(
        TEST_ASSET_NAME_MICROSOFT,
        support.firstNonBlank(TEST_BLANK_VALUE, TEST_ASSET_NAME_MICROSOFT));
  }

  @Test
  void firstNonNullReturnsPrimaryWhenPresent() {
    assertEquals(AssetTypeEnum.STOCK, support.firstNonNull(AssetTypeEnum.STOCK, AssetTypeEnum.ETF));
  }

  @Test
  void firstNonNullReturnsFallbackWhenPrimaryIsNull() {
    assertEquals(AssetTypeEnum.ETF, support.firstNonNull(null, AssetTypeEnum.ETF));
  }

  @Test
  void hasMeaningfulFieldReturnsFalseWhenAllFieldsAreEmpty() {
    AssetImportCandidateDto asset =
        new AssetImportCandidateDto(TEST_BLANK_VALUE, TEST_BLANK_VALUE, null, null, null, null);

    assertFalse(support.hasMeaningfulField(asset));
  }

  @Test
  void hasMeaningfulFieldReturnsTrueWhenAnyFieldIsPresent() {
    assertTrue(support.hasMeaningfulField(validImportedAssetCandidate()));
  }

  @Test
  void mergeAssetPairKeepsAiTickerWhenExtractionProvidesIsin() {
    AssetImportCandidateDto aiAsset =
        new AssetImportCandidateDto(
            TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL, AssetTypeEnum.STOCK, null, null, null);
    AssetImportCandidateDto extractedAsset =
        new AssetImportCandidateDto(
            null,
            TEST_ASSET_ISIN_APPLE,
            AssetTypeEnum.OTHER,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    AssetImportCandidateDto merged = support.mergeAssetPair(aiAsset, extractedAsset);

    assertEquals(TEST_ASSET_NAME_APPLE, merged.getName());
    assertEquals(TEST_ASSET_SYMBOL_AAPL, merged.getSymbol());
    assertEquals(AssetTypeEnum.STOCK, merged.getAssetType());
    assertEquals(TEST_ASSET_RESPONSE_QUANTITY, merged.getQuantity());
    assertEquals(TEST_ASSET_RESPONSE_AVERAGE_PRICE, merged.getAveragePrice());
    assertEquals(CurrencyCodeEnum.USD, merged.getCurrency());
  }

  @Test
  void buildMergedAssetPrefersExtractedValuesAndSpecificType() {
    AssetImportCandidateDto aiAsset =
        new AssetImportCandidateDto(
            TEST_ASSET_NAME_MICROSOFT, null, AssetTypeEnum.OTHER, null, null, null);
    AssetImportCandidateDto extractedAsset =
        new AssetImportCandidateDto(
            TEST_ASSET_NAME_APPLE,
            TEST_ASSET_ISIN_APPLE,
            AssetTypeEnum.STOCK,
            TEST_ASSET_RESPONSE_QUANTITY,
            TEST_ASSET_RESPONSE_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);

    AssetImportCandidateDto merged =
        support.buildMergedAsset(
            aiAsset,
            extractedAsset,
            TEST_ASSET_ISIN_APPLE,
            AssetTypeEnum.STOCK,
            AssetTypeEnum.OTHER);

    assertEquals(TEST_ASSET_NAME_APPLE, merged.getName());
    assertEquals(TEST_ASSET_ISIN_APPLE, merged.getSymbol());
    assertEquals(AssetTypeEnum.STOCK, merged.getAssetType());
    assertEquals(TEST_ASSET_RESPONSE_QUANTITY, merged.getQuantity());
    assertEquals(TEST_ASSET_RESPONSE_AVERAGE_PRICE, merged.getAveragePrice());
    assertEquals(CurrencyCodeEnum.USD, merged.getCurrency());
  }

  @Test
  void buildMergedAssetFallsBackToAiTypeWhenExtractedTypeIsOther() {
    AssetImportCandidateDto aiAsset =
        new AssetImportCandidateDto(
            TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL, AssetTypeEnum.STOCK, null, null, null);
    AssetImportCandidateDto extractedAsset =
        new AssetImportCandidateDto(
            null, TEST_ASSET_ISIN_APPLE, AssetTypeEnum.OTHER, null, null, null);

    AssetImportCandidateDto merged =
        support.buildMergedAsset(
            aiAsset,
            extractedAsset,
            TEST_ASSET_ISIN_APPLE,
            AssetTypeEnum.OTHER,
            AssetTypeEnum.STOCK);

    assertEquals(AssetTypeEnum.STOCK, merged.getAssetType());
    assertNull(merged.getQuantity());
    assertNull(merged.getAveragePrice());
    assertNull(merged.getCurrency());
  }
}
