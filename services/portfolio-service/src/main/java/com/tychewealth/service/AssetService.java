package com.tychewealth.service;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AssetService {

  AssetImportResponseDto importAssets(Long userId, MultipartFile file);
}
