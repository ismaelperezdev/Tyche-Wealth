package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_URL;
import static com.tychewealth.constants.ApiConstants.MULTIPART_FORM_DATA;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.asset.AssetImportResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping
@Tag(name = "Asset")
public interface AssetApi {

  @PostMapping(
      value = ASSET_IMPORT_URL,
      consumes = MULTIPART_FORM_DATA,
      produces = REQUEST_PRODUCES)
  ResponseEntity<AssetImportResponseDto> importAssets(
      @AuthenticationPrincipal Long userId, @RequestPart("file") MultipartFile file);
}
