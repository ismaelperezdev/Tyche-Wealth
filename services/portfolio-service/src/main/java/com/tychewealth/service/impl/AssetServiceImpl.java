package com.tychewealth.service.impl;

import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_RESULT_KEY_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetImportPayloadDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.AssetService;
import com.tychewealth.service.helper.CommonValidationHelper;
import com.tychewealth.service.helper.asset.AssetCreateHelper;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ImportAssetsHelper;
import com.tychewealth.service.helper.asset.ai.AiResponseParser;
import com.tychewealth.service.helper.asset.ai.AssetAiValidationHelper;
import com.tychewealth.service.helper.asset.ai.ImportAssetsAiHelper;
import com.tychewealth.utils.Utils;
import com.tychewealth.utils.prompts.AssetImportPromptUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  private final AssetCreateHelper assetCreateHelper;
  private final AssetValidationHelper assetValidationHelper;
  private final AssetAiValidationHelper assetAiValidationHelper;
  private final CommonValidationHelper commonValidationHelper;
  private final ImportAssetsAiHelper importAssetsAiHelper;
  private final ImportAssetsHelper importAssetsHelper;
  private final AiResponseParser aiResponseParser;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public AssetResponseDto create(
      Long userId, Long portfolioId, AssetCreateRequestDto createRequest) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    PortfolioEntity portfolio =
        commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, RETRIEVE_ACTION);
    assetValidationHelper.validateCreateLimit(portfolioId);
    assetValidationHelper.validateCreateNameConflict(portfolioId, createRequest.getName());
    try {
      return assetCreateHelper.create(portfolio, createRequest);
    } catch (DataIntegrityViolationException ex) {
      throw assetValidationHelper.translateSymbolPersistenceConflict(
          ex, portfolioId, createRequest.getSymbol());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public AssetImportResponseDto importAssets(Long userId, MultipartFile file) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    assetAiValidationHelper.validateImportRequest(file);
    AssetImportPayloadDto payload = importAssetsHelper.buildImportPayload(file);
    String prompt = AssetImportPromptUtils.buildAssetImportPrompt(payload.getExtractedText());
    String aiResponse = importAssetsAiHelper.prompt(prompt, AiModelTypeEnum.FAST);
    String importId;
    try {
      importId =
          Utils.sha256Hex(
              payload.getFileName().toLowerCase(Locale.ROOT)
                  + ":"
                  + Utils.sha256Hex(file.getBytes()));
    } catch (IOException ex) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED, Map.of(), HttpStatus.BAD_REQUEST);
    }
    AssetImportResponseDto response =
        new AssetImportResponseDto(
            importId, aiResponseParser.parseAiAssets(payload.getExtractedText(), aiResponse));

    importAssetsHelper.savePersistedImportResult(
        new AssetPersistRedisDto(importId, userId, payload.getFileName(), Instant.now(), response));

    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public AssetImportResponseDto retrieveImportedAssets(Long userId, String importId) {
    commonValidationHelper.validateAuthenticatedUser(userId);

    String persistedImportJson =
        redisTemplate.opsForValue().get(ASSET_IMPORT_RESULT_KEY_PREFIX + userId + ":" + importId);
    if (persistedImportJson == null || persistedImportJson.isBlank()) {
      assetAiValidationHelper.validateRetrievedImportExists(null);
    }

    try {
      AssetPersistRedisDto persistedImport =
          objectMapper.readValue(persistedImportJson, AssetPersistRedisDto.class);
      assetAiValidationHelper.validateRetrievedImportExists(persistedImport);
      return persistedImport.getResult();
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to read persisted asset import result", ex);
    }
  }
}
