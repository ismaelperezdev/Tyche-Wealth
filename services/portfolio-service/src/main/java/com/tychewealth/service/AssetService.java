package com.tychewealth.service;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetBatchCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

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
