package com.tychewealth.service.helper.asset;

import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.mapper.asset.AssetMapper;
import com.tychewealth.repository.AssetRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AssetCreateHelper {

  private final AssetRepository assetRepository;
  private final AssetMapper assetMapper;

  public AssetResponseDto create(PortfolioEntity portfolio, AssetCreateRequestDto createRequest) {
    AssetEntity asset = assetMapper.create(createRequest);
    asset.setPortfolio(portfolio);

    AssetEntity persistedAsset = assetRepository.saveAndFlush(asset);
    return assetMapper.toDto(persistedAsset);
  }
}
