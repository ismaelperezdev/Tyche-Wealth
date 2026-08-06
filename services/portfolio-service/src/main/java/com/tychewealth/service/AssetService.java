package com.tychewealth.service;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetBatchCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application service contract for authenticated portfolio-asset operations.
 *
 * <p>Defines the use cases for creating, listing, retrieving, updating, and deleting assets, as
 * well as importing asset data and confirming or discarding imported candidates while keeping
 * validation, persistence, AI processing, and temporary-result handling behind the service
 * boundary.
 */
public interface AssetService {

  AssetResponseDto create(Long userId, Long portfolioId, AssetCreateRequestDto createRequest);

  Page<AssetResponseDto> listAssets(Long userId, Long portfolioId, int page, int limit);

  AssetResponseDto retrieve(Long userId, Long portfolioId, Long assetId);

  AssetResponseDto update(
      Long userId, Long portfolioId, Long assetId, AssetUpdateRequestDto updateRequest);

  void delete(Long userId, Long portfolioId, Long assetId);

  List<AssetResponseDto> createBatchFromImportedAssets(
      Long userId, Long portfolioId, AssetBatchCreateRequestDto request);

  AssetImportResponseDto importAssets(Long userId, MultipartFile file);

  AssetImportResponseDto retrieveImportedAssets(Long userId, String importId);
}
