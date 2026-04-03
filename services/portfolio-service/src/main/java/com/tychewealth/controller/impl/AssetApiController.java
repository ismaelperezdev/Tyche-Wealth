package com.tychewealth.controller.impl;

import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static org.springframework.http.ResponseEntity.status;

import com.tychewealth.controller.AssetApi;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimited;
import com.tychewealth.service.AssetService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@AllArgsConstructor
public class AssetApiController implements AssetApi {

  private final AssetService assetService;

  @Override
  @RateLimited(RateLimitKey.ASSET_IMPORT)
  public ResponseEntity<AssetImportResponseDto> importAssets(
      @AuthenticationPrincipal Long userId, @RequestPart("file") MultipartFile file) {
    log.info(REQUEST_START + USER_ID, ASSET, IMPORT_ASSETS_ACTION, userId);

    AssetImportResponseDto response = assetService.importAssets(userId, file);
    log.info(REQUEST_SUCCESS + USER_ID, ASSET, IMPORT_ASSETS_ACTION, userId);

    return status(HttpStatus.OK).body(response);
  }
}
