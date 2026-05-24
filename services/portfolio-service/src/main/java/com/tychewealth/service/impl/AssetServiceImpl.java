package com.tychewealth.service.impl;

import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.DELETE_ACTION;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.LogConstants.UPDATE_ACTION;
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_RESULT_KEY_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.ai.AiModelTypeEnum;
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
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.asset.AssetMapper;
import com.tychewealth.repository.AssetRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  private final AssetRepository assetRepository;
  private final AssetCreateHelper assetCreateHelper;
  private final AssetMapper assetMapper;
  private final AssetValidationHelper assetValidationHelper;
  private final AssetAiValidationHelper assetAiValidationHelper;
  private final CommonValidationHelper commonValidationHelper;
  private final ImportAssetsAiHelper importAssetsAiHelper;
  private final ImportAssetsHelper importAssetsHelper;
  private final AiResponseParser aiResponseParser;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public AssetResponseDto create(
      Long userId, Long portfolioId, AssetCreateRequestDto createRequest) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    PortfolioEntity portfolio =
        commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, CREATE_ACTION);
    assetValidationHelper.validateCreateLimit(portfolioId);
    assetValidationHelper.validateCreateNameConflict(portfolioId, createRequest.getName());
    assetValidationHelper.validateCreateSymbolConflict(portfolioId, createRequest.getSymbol());
    try {
      return assetCreateHelper.create(portfolio, createRequest);
    } catch (DataIntegrityViolationException ex) {
      throw assetValidationHelper.translateSymbolPersistenceConflict(
          ex, portfolioId, createRequest.getSymbol());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public AssetResponseDto retrieve(Long userId, Long portfolioId, Long assetId) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, RETRIEVE_ACTION);
    AssetEntity asset = assetValidationHelper.validateRetrievedAssetExists(portfolioId, assetId);
    return assetMapper.toDto(asset);
  }

  @Override
  @Transactional
  public void delete(Long userId, Long portfolioId, Long assetId) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, DELETE_ACTION);
    assetRepository.deleteByIdAndPortfolioId(assetId, portfolioId);
  }

  @Override
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public AssetResponseDto update(
      Long userId, Long portfolioId, Long assetId, AssetUpdateRequestDto updateRequest) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, UPDATE_ACTION);
    AssetEntity asset = assetValidationHelper.validateRetrievedAssetExists(portfolioId, assetId);

    if (updateRequest.getName() != null && !updateRequest.getName().equals(asset.getName())) {
      assetValidationHelper.validateCreateNameConflict(portfolioId, updateRequest.getName());
    }
    if (updateRequest.getSymbol() != null && !updateRequest.getSymbol().equals(asset.getSymbol())) {
      assetValidationHelper.validateCreateSymbolConflict(portfolioId, updateRequest.getSymbol());
    }

    assetMapper.update(updateRequest, asset);
    return assetMapper.toDto(assetRepository.saveAndFlush(asset));
  }

  @Override
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public List<AssetResponseDto> createBatchFromImportedAssets(
      Long userId, Long portfolioId, AssetBatchCreateRequestDto request) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    PortfolioEntity portfolio =
        commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, CREATE_ACTION);

    assetValidationHelper.validateBatchCreateRequest(
        request.getAction(), request.getImportId(), request.getAssets());

    if (request.getAction() == AssetBatchActionEnum.DISCARD) {
      cleanupImportedResult(userId, request.getImportId());
      return List.of();
    }

    List<AssetCreateRequestDto> assets = request.getAssets();
    assetValidationHelper.validateBatchCreateLimit(portfolioId, assets.size());
    assetValidationHelper.validateBatchCreateRequestDuplicates(assets);
    assetValidationHelper.validateBatchCreateDatabaseConflicts(portfolioId, assets);

    try {
      List<AssetResponseDto> createdAssets = assetCreateHelper.createBatch(portfolio, assets);
      cleanupImportedResult(userId, request.getImportId());
      return createdAssets;
    } catch (DataIntegrityViolationException ex) {
      String symbol = assets.isEmpty() ? null : assets.getFirst().getSymbol();
      throw assetValidationHelper.translateSymbolPersistenceConflict(ex, portfolioId, symbol);
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

  private void cleanupImportedResult(Long userId, String importId) {
    String sanitizedImportId = Utils.trimToNull(importId);
    if (sanitizedImportId == null) {
      return;
    }
    redisTemplate.delete(ASSET_IMPORT_RESULT_KEY_PREFIX + userId + ":" + sanitizedImportId);
  }
}
