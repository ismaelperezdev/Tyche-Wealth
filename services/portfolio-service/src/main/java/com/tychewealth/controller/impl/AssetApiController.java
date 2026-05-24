package com.tychewealth.controller.impl;

import static com.tychewealth.constants.ApiConstants.DEFAULT_ASSET_LIST_LIMIT;
import static com.tychewealth.constants.ApiConstants.DEFAULT_PAGE;
import static com.tychewealth.constants.ApiConstants.IMPORT_ID_PATH;
import static com.tychewealth.constants.ApiConstants.LIMIT_PARAM;
import static com.tychewealth.constants.ApiConstants.PAGE_PARAM;
import static com.tychewealth.constants.ApiConstants.X_HAS_NEXT_HEADER;
import static com.tychewealth.constants.ApiConstants.X_LIMIT_HEADER;
import static com.tychewealth.constants.ApiConstants.X_PAGE_HEADER;
import static com.tychewealth.constants.ApiConstants.X_TOTAL_COUNT_HEADER;
import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.DELETE_ACTION;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.LIST_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_ID;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.LogConstants.UPDATE_ACTION;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.utils.Utils.buildNoStoreBodyResponse;
import static com.tychewealth.utils.Utils.buildNoStoreEmptyResponse;

import com.tychewealth.controller.AssetApi;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetBatchCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimited;
import com.tychewealth.service.AssetService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
  @RateLimited(RateLimitKey.ASSET_LIST)
  public ResponseEntity<List<AssetResponseDto>> listAssets(
      @AuthenticationPrincipal Long userId,
      Long portfolioId,
      @RequestParam(name = PAGE_PARAM, defaultValue = DEFAULT_PAGE) int page,
      @RequestParam(name = LIMIT_PARAM, defaultValue = DEFAULT_ASSET_LIST_LIMIT) int limit) {
    log.info(
        REQUEST_START + PORTFOLIO_ID + USER_ID, ASSET, LIST_ASSETS_ACTION, portfolioId, userId);

    Page<AssetResponseDto> response = assetService.listAssets(userId, portfolioId, page, limit);
    log.info(
        REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, ASSET, LIST_ASSETS_ACTION, portfolioId, userId);

    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .header(X_TOTAL_COUNT_HEADER, String.valueOf(response.getTotalElements()))
        .header(X_PAGE_HEADER, String.valueOf(page))
        .header(X_LIMIT_HEADER, String.valueOf(limit))
        .header(X_HAS_NEXT_HEADER, String.valueOf(response.hasNext()))
        .body(response.getContent());
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
  @RateLimited(RateLimitKey.ASSET_UPDATE)
  public ResponseEntity<AssetResponseDto> update(
      @AuthenticationPrincipal Long userId,
      Long portfolioId,
      Long assetId,
      @Valid @RequestBody AssetUpdateRequestDto updateRequest) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, ASSET, UPDATE_ACTION, portfolioId, userId);

    AssetResponseDto response = assetService.update(userId, portfolioId, assetId, updateRequest);
    log.info(REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, ASSET, UPDATE_ACTION, portfolioId, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }

  @Override
  @RateLimited(RateLimitKey.ASSET_DELETE)
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Long userId, Long portfolioId, Long assetId) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, ASSET, DELETE_ACTION, portfolioId, userId);

    assetService.delete(userId, portfolioId, assetId);
    log.info(REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, ASSET, DELETE_ACTION, portfolioId, userId);

    return buildNoStoreEmptyResponse(HttpStatus.NO_CONTENT);
  }

  @Override
  @RateLimited(RateLimitKey.ASSET_BATCH_CREATE)
  public ResponseEntity<List<AssetResponseDto>> createBatchFromImportedAssets(
      @AuthenticationPrincipal Long userId,
      Long portfolioId,
      @Valid @RequestBody AssetBatchCreateRequestDto request) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, ASSET, CREATE_ACTION, portfolioId, userId);

    List<AssetResponseDto> response =
        assetService.createBatchFromImportedAssets(userId, portfolioId, request);
    log.info(REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, ASSET, CREATE_ACTION, portfolioId, userId);

    return buildNoStoreBodyResponse(HttpStatus.CREATED, response);
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
      @AuthenticationPrincipal Long userId, @PathVariable(IMPORT_ID_PATH) String importId) {
    log.info(REQUEST_START + USER_ID, ASSET, RETRIEVE_ACTION, userId);

    AssetImportResponseDto response = assetService.retrieveImportedAssets(userId, importId);
    log.info(REQUEST_SUCCESS + USER_ID, ASSET, RETRIEVE_ACTION, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }
}
