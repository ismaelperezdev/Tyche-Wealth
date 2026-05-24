package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.ASSET_ID_PATH;
import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_BY_ID_URL;
import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_URL;
import static com.tychewealth.constants.ApiConstants.IMPORT_ID_PATH;
import static com.tychewealth.constants.ApiConstants.MULTIPART_FORM_DATA;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping
@Tag(name = "Asset")
public interface AssetApi {

  @PostMapping(
      value = PORTFOLIO_ASSET_BASE_URL,
      consumes = REQUEST_CONSUMES,
      produces = REQUEST_PRODUCES)
  ResponseEntity<AssetResponseDto> create(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @Valid @RequestBody AssetCreateRequestDto createRequest);

  @GetMapping(value = PORTFOLIO_ASSET_BY_ID_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<AssetResponseDto> retrieve(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @PathVariable(ASSET_ID_PATH) Long assetId);

  @PostMapping(
      value = PORTFOLIO_ASSET_BATCH_URL,
      consumes = REQUEST_CONSUMES,
      produces = REQUEST_PRODUCES)
  ResponseEntity<List<AssetResponseDto>> createBatchFromImportedAssets(
      @AuthenticationPrincipal Long userId,
      @PathVariable(PORTFOLIO_ID_PATH) Long portfolioId,
      @Valid @RequestBody AssetBatchCreateRequestDto request);

  @PostMapping(
      value = ASSET_IMPORT_URL,
      consumes = MULTIPART_FORM_DATA,
      produces = REQUEST_PRODUCES)
  ResponseEntity<AssetImportResponseDto> importAssets(
      @AuthenticationPrincipal Long userId, @RequestPart("file") MultipartFile file);

  @GetMapping(value = ASSET_IMPORT_BY_ID_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<AssetImportResponseDto> retrieveImportedAssets(
      @AuthenticationPrincipal Long userId, @PathVariable(IMPORT_ID_PATH) String importId);
}
