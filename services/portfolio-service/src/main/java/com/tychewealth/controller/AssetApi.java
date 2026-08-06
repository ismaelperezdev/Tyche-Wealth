package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.ASSET_ID_PATH;
import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_BY_ID_URL;
import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_URL;
import static com.tychewealth.constants.ApiConstants.DEFAULT_LIST_LIMIT;
import static com.tychewealth.constants.ApiConstants.DEFAULT_PAGE;
import static com.tychewealth.constants.ApiConstants.IMPORT_ID_PATH;
import static com.tychewealth.constants.ApiConstants.LIMIT_PARAM;
import static com.tychewealth.constants.ApiConstants.MULTIPART_FORM_DATA;
import static com.tychewealth.constants.ApiConstants.PAGE_PARAM;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ASSET_BASE_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ASSET_BATCH_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ASSET_BY_ID_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ID_PATH;
import static com.tychewealth.constants.ApiConstants.REQUEST_CONSUMES;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetBatchCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines the HTTP contract for portfolio asset management and asset imports.
 *
 * <p>Declares endpoints for creating, listing, retrieving, updating, and deleting assets, as well
 * as starting and retrieving asset imports.
 */
@RequestMapping
@Tag(name = "Asset")
public interface AssetApi {

  /** Creates an asset in the specified portfolio. */
  @PostMapping(
      value = PORTFOLIO_ASSET_BASE_URL,
      consumes = REQUEST_CONSUMES,
      produces = REQUEST_PRODUCES)
  ResponseEntity<AssetResponseDto> create(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @Valid @RequestBody AssetCreateRequestDto createRequest);

  /** Returns a paginated list of assets belonging to the specified portfolio. */
  @GetMapping(value = PORTFOLIO_ASSET_BASE_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<AssetResponseDto>> listAssets(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @RequestParam(name = PAGE_PARAM, defaultValue = DEFAULT_PAGE) int page,
      @RequestParam(name = LIMIT_PARAM, defaultValue = DEFAULT_LIST_LIMIT) int limit);

  /** Returns a single asset from the specified portfolio. */
  @GetMapping(value = PORTFOLIO_ASSET_BY_ID_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<AssetResponseDto> retrieve(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @PathVariable(ASSET_ID_PATH) Long assetId);

  /** Deletes an asset from the specified portfolio. */
  @DeleteMapping(value = PORTFOLIO_ASSET_BY_ID_URL)
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @PathVariable(ASSET_ID_PATH) Long assetId);

  /** Updates an existing asset in the specified portfolio. */
  @PatchMapping(
      value = PORTFOLIO_ASSET_BY_ID_URL,
      consumes = REQUEST_CONSUMES,
      produces = REQUEST_PRODUCES)
  ResponseEntity<AssetResponseDto> update(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @PathVariable(ASSET_ID_PATH) Long assetId,
      @Valid @RequestBody AssetUpdateRequestDto updateRequest);

  /** Creates multiple assets in a portfolio from a completed asset import. */
  @PostMapping(
      value = PORTFOLIO_ASSET_BATCH_URL,
      consumes = REQUEST_CONSUMES,
      produces = REQUEST_PRODUCES)
  ResponseEntity<List<AssetResponseDto>> createBatchFromImportedAssets(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @Valid @RequestBody AssetBatchCreateRequestDto request);

  /** Starts importing assets from the supplied multipart file. */
  @PostMapping(
      value = ASSET_IMPORT_URL,
      consumes = MULTIPART_FORM_DATA,
      produces = REQUEST_PRODUCES)
  ResponseEntity<AssetImportResponseDto> importAssets(
      @AuthenticationPrincipal Long userId, @RequestPart("file") MultipartFile file);

  /** Returns the current status and results of an asset import. */
  @GetMapping(value = ASSET_IMPORT_BY_ID_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<AssetImportResponseDto> retrieveImportedAssets(
      @AuthenticationPrincipal Long userId, @PathVariable(IMPORT_ID_PATH) String importId);
}
