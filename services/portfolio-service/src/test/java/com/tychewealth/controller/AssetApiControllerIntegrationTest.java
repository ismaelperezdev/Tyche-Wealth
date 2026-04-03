package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.CommonConstants.DESCRIPTION;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AssetTestData.AI_RESPONSE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_CONTENT_TYPE_CSV;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_EXTRACTED_TEXT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_FILE_NAME;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_AVERAGE_PRICE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_RESPONSE_QUANTITY;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.validImportedAssetCandidate;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.config.AssetIntegrationTestConfig;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.asset.ImportAssetsAiHelper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AssetIntegrationTestConfig.class)
@ContextConfiguration(initializers = AssetIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class AssetApiControllerIntegrationTest {

  private static final int ATTACHMENT_SIZE_LIMIT_BYTES = 3_145_728;

  @Autowired private MockMvc mockMvc;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;

  @MockitoBean private ImportAssetsAiHelper importAssetsAiHelper;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();

    when(importAssetsAiHelper.promptFast(anyString())).thenReturn(AI_RESPONSE);
    when(importAssetsAiHelper.parseAiAssets(AI_RESPONSE))
        .thenReturn(List.of(validImportedAssetCandidate()));
  }

  @Test
  void importReturnsOkWhenAttachmentIsValid() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes(UTF_8));

    mockMvc
        .perform(
            multipart(ASSET_IMPORT_URL)
                .file(file)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.fileName").value(TEST_ASSET_FILE_NAME))
        .andExpect(jsonPath("$.extractedText").value(TEST_ASSET_EXTRACTED_TEXT))
        .andExpect(jsonPath("$.aiResponse").value(containsString(TEST_ASSET_NAME_APPLE)))
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
  void importReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            TEST_ASSET_FILE_NAME,
            TEST_ASSET_CONTENT_TYPE_CSV,
            TEST_ASSET_EXTRACTED_TEXT.getBytes(UTF_8));

    mockMvc
        .perform(multipart(ASSET_IMPORT_URL).file(file))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION).value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void importReturnsBadRequestWhenAttachmentIsEmpty() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", TEST_ASSET_FILE_NAME, TEST_ASSET_CONTENT_TYPE_CSV, new byte[0]);

    mockMvc
        .perform(
            multipart(ASSET_IMPORT_URL)
                .file(file)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("file must not be empty")));
  }

  @Test
  void importReturnsBadRequestWhenAttachmentExceedsSizeLimit() throws Exception {
    byte[] oversizedContent = new byte[ATTACHMENT_SIZE_LIMIT_BYTES + 1];
    MockMultipartFile file =
        new MockMultipartFile(
            "file", TEST_ASSET_FILE_NAME, TEST_ASSET_CONTENT_TYPE_CSV, oversizedContent);

    mockMvc
        .perform(
            multipart(ASSET_IMPORT_URL)
                .file(file)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED.getCode()))
        .andExpect(
            jsonPath("$.type").value(ErrorDefinition.ATTACHMENT_SIZE_LIMIT_EXCEEDED.getType()));
  }
}
