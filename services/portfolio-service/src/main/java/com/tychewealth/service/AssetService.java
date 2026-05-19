package com.tychewealth.service;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import org.springframework.web.multipart.MultipartFile;

public interface AssetService {

  AssetResponseDto create(Long userId, Long portfolioId, AssetCreateRequestDto createRequest);

  AssetResponseDto retrieve(Long userId, Long portfolioId, Long assetId);

  AssetImportResponseDto importAssets(Long userId, MultipartFile file);

  AssetImportResponseDto retrieveImportedAssets(Long userId, String importId);
}
