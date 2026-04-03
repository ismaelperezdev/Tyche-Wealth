package com.tychewealth.service.impl;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.service.AssetService;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ImportAssetsAiHelper;
import com.tychewealth.service.helper.asset.ImportAssetsHelper;
import com.tychewealth.utils.prompts.AssetImportPromptUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  private final AssetValidationHelper assetValidationHelper;
  private final ImportAssetsAiHelper importAssetsAiHelper;
  private final ImportAssetsHelper importAssetsHelper;

  @Override
  @Transactional(readOnly = true)
  public AssetImportResponseDto importAssets(Long userId, MultipartFile file) {
    assetValidationHelper.validateImportRequest(userId, file);
    AssetImportResponseDto response = importAssetsHelper.buildImportPayload(file);
    String prompt = AssetImportPromptUtils.buildAssetImportPrompt(response.getExtractedText());
    String aiResponse = importAssetsAiHelper.promptFast(prompt);
    response.setAiResponse(aiResponse);
    response.setAssets(importAssetsAiHelper.parseAiAssets(aiResponse));
    return response;
  }
}
