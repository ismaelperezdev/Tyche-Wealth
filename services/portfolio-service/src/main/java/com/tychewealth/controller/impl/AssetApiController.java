package com.tychewealth.controller.impl;

import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_ID;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static com.tychewealth.utils.Utils.buildNoStoreBodyResponse;

import com.tychewealth.controller.AssetApi;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimited;
import com.tychewealth.service.AssetService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@AllArgsConstructor
public class AssetApiController implements AssetApi {

  private final AssetService assetService;

  @Override
  @RateLimited(RateLimitKey.ASSET_CREATE)
  public ResponseEntity<AssetResponseDto> create(
      @AuthenticationPrincipal Long userId,
      Long portfolioId,
      @Valid @RequestBody AssetCreateRequestDto createRequest) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, ASSET, CREATE_ACTION, portfolioId, userId);

    AssetResponseDto response = assetService.create(userId, portfolioId, createRequest);
    log.info(REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, ASSET, CREATE_ACTION, portfolioId, userId);

    return buildNoStoreBodyResponse(HttpStatus.CREATED, response);
  }

  @Override
  @RateLimited(RateLimitKey.ASSET_RETRIEVE)
  public ResponseEntity<AssetResponseDto> retrieve(
      @AuthenticationPrincipal Long userId, Long portfolioId, Long assetId) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, ASSET, RETRIEVE_ACTION, portfolioId, userId);

    AssetResponseDto response = assetService.retrieve(userId, portfolioId, assetId);
    log.info(REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, ASSET, RETRIEVE_ACTION, portfolioId, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }

  @Override
  @RateLimited(RateLimitKey.ASSET_IMPORT)
  public ResponseEntity<AssetImportResponseDto> importAssets(
      @AuthenticationPrincipal Long userId, @RequestPart("file") MultipartFile file) {
    log.info(REQUEST_START + USER_ID, ASSET, IMPORT_ASSETS_ACTION, userId);

    AssetImportResponseDto response = assetService.importAssets(userId, file);
    log.info(REQUEST_SUCCESS + USER_ID, ASSET, IMPORT_ASSETS_ACTION, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }

  @Override
  @RateLimited(RateLimitKey.ASSET_IMPORT_RETRIEVE)
  public ResponseEntity<AssetImportResponseDto> retrieveImportedAssets(
      @AuthenticationPrincipal Long userId, @PathVariable String importId) {
    log.info(REQUEST_START + USER_ID, ASSET, RETRIEVE_ACTION, userId);

    AssetImportResponseDto response = assetService.retrieveImportedAssets(userId, importId);
    log.info(REQUEST_SUCCESS + USER_ID, ASSET, RETRIEVE_ACTION, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }
}
