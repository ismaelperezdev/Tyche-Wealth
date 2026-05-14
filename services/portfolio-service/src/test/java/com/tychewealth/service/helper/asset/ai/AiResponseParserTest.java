package com.tychewealth.service.helper.asset.ai;

import static com.tychewealth.testdata.AiTestData.TEST_AI_RESPONSE_INVALID_JSON;
import static com.tychewealth.testdata.AiTestData.TEST_AI_RESPONSE_MARKDOWN_WITH_TRAILING_TEXT;
import static com.tychewealth.testdata.AiTestData.TEST_AI_RESPONSE_TWO_STOCKS;
import static com.tychewealth.testdata.AiTestData.TEST_AI_RESPONSE_WITH_NULL_AND_EMPTY_CANDIDATES;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.testdata.AiTestData.TEST_EXTRACTED_STATEMENT_SINGLE_HOLDING;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_DETECTED_ASSETS;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_DETECTED_ASSETS_TIGHT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_AVERAGE_PRICE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_QUANTITY;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testhelper.AiTestHelper.buildAiResponseParser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.helper.asset.ai.support.AiResponseParserSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AiResponseParserTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AiResponseParserSupport parserSupport = new AiResponseParserSupport(objectMapper);

  @Test
  void parseAiAssetsSanitizesMarkdownAndParsesCandidates() {
    AiResponseParser parser =
        buildAiResponseParser(objectMapper, parserSupport, TEST_MAX_DETECTED_ASSETS);

    List<AssetImportCandidateDto> assets =
        parser.parseAiAssets(null, TEST_AI_RESPONSE_MARKDOWN_WITH_TRAILING_TEXT);

    assertEquals(1, assets.size());
    assertEquals(TEST_ASSET_NAME_APPLE, assets.getFirst().getName());
    assertEquals(TEST_ASSET_SYMBOL_AAPL, assets.getFirst().getSymbol());
    assertEquals(AssetTypeEnum.STOCK, assets.getFirst().getAssetType());
    assertEquals(TEST_ASSET_RESPONSE_QUANTITY, assets.getFirst().getQuantity());
    assertEquals(TEST_ASSET_RESPONSE_AVERAGE_PRICE, assets.getFirst().getAveragePrice());
    assertEquals(CurrencyCodeEnum.USD, assets.getFirst().getCurrency());
  }

  @Test
  void parseAiAssetsFiltersNullAndMeaninglessCandidates() {
    AiResponseParser parser =
        buildAiResponseParser(objectMapper, parserSupport, TEST_MAX_DETECTED_ASSETS);

    List<AssetImportCandidateDto> assets =
        parser.parseAiAssets(null, TEST_AI_RESPONSE_WITH_NULL_AND_EMPTY_CANDIDATES);

    assertEquals(1, assets.size());
    assertEquals(TEST_ASSET_NAME_APPLE, assets.getFirst().getName());
    assertEquals(TEST_ASSET_SYMBOL_AAPL, assets.getFirst().getSymbol());
    assertEquals(AssetTypeEnum.STOCK, assets.getFirst().getAssetType());
    assertNull(assets.getFirst().getQuantity());
    assertNull(assets.getFirst().getAveragePrice());
    assertNull(assets.getFirst().getCurrency());
  }

  @Test
  void parseAiAssetsKeepsAiPayloadWhenDeterministicExtractionCountDiffers() {
    AiResponseParser parser =
        buildAiResponseParser(objectMapper, parserSupport, TEST_MAX_DETECTED_ASSETS);

    List<AssetImportCandidateDto> assets =
        parser.parseAiAssets(TEST_EXTRACTED_STATEMENT_SINGLE_HOLDING, TEST_AI_RESPONSE_TWO_STOCKS);

    assertEquals(2, assets.size());
    assertEquals(TEST_ASSET_NAME_APPLE, assets.getFirst().getName());
    assertEquals(TEST_ASSET_SYMBOL_AAPL, assets.getFirst().getSymbol());
    assertEquals(TEST_ASSET_NAME_MICROSOFT, assets.get(1).getName());
    assertEquals(TEST_ASSET_SYMBOL_MSFT, assets.get(1).getSymbol());
  }

  @Test
  void parseAiAssetsThrowsBadRequestWhenAiResponseIsInvalidJson() {
    AiResponseParser parser =
        buildAiResponseParser(objectMapper, parserSupport, TEST_MAX_DETECTED_ASSETS);

    AssetImportException exception =
        assertThrows(
            AssetImportException.class,
            () -> parser.parseAiAssets(null, TEST_AI_RESPONSE_INVALID_JSON));

    assertEquals(ErrorDefinition.ASSET_IMPORT_AI_RESPONSE_INVALID, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void parseAiAssetsThrowsWhenDetectedAssetCountExceedsConfiguredLimit() {
    AiResponseParser parser =
        buildAiResponseParser(objectMapper, parserSupport, TEST_MAX_DETECTED_ASSETS_TIGHT);

    AssetImportException exception =
        assertThrows(
            AssetImportException.class,
            () -> parser.parseAiAssets(null, TEST_AI_RESPONSE_TWO_STOCKS));

    assertEquals(
        ErrorDefinition.ASSET_IMPORT_RESULT_LIMIT_EXCEEDED, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }
}
