package com.tychewealth.service.assetvariation;

import com.tychewealth.dto.asset.AssetVariationDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.enums.AssetVariationTypeEnum;
import com.tychewealth.mapper.assetvariation.AssetVariationMapper;
import com.tychewealth.repository.AssetVariationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/** Records immutable variations produced by asset lifecycle and holding changes. */
@Service
@AllArgsConstructor
public class AssetVariationService {

  private final AssetVariationRepository assetVariationRepository;
  private final AssetVariationMapper assetVariationMapper;

  public void create(AssetEntity asset, LocalDateTime occurredAt) {
    save(
        asset,
        AssetVariationTypeEnum.CREATED,
        null,
        asset.getQuantity(),
        null,
        asset.getAveragePrice(),
        occurredAt);
  }

  public void createAll(Collection<AssetEntity> assets, LocalDateTime occurredAt) {
    if (assets == null || assets.isEmpty()) {
      return;
    }

    List<AssetVariationEntity> variations = new ArrayList<>(assets.size());
    for (AssetEntity asset : assets) {
      AssetVariationDto variationDto =
          new AssetVariationDto(
              null,
              asset.getId(),
              AssetVariationTypeEnum.CREATED,
              null,
              asset.getQuantity(),
              null,
              asset.getAveragePrice(),
              occurredAt);
      variations.add(assetVariationMapper.create(variationDto, asset));
    }

    assetVariationRepository.saveAll(variations);
  }

  public void update(
      AssetEntity asset,
      BigDecimal previousQuantity,
      BigDecimal previousAveragePrice,
      LocalDateTime occurredAt) {
    save(
        asset,
        AssetVariationTypeEnum.MANUAL_UPDATE,
        previousQuantity,
        asset.getQuantity(),
        previousAveragePrice,
        asset.getAveragePrice(),
        occurredAt);
  }

  public void delete(AssetEntity asset, LocalDateTime occurredAt) {
    save(
        asset,
        AssetVariationTypeEnum.DELETED,
        asset.getQuantity(),
        null,
        asset.getAveragePrice(),
        null,
        occurredAt);
  }

  public void deleteAll(Collection<AssetEntity> assets, LocalDateTime occurredAt) {
    if (assets == null || assets.isEmpty()) {
      return;
    }

    List<AssetVariationEntity> variations = new ArrayList<>(assets.size());
    for (AssetEntity asset : assets) {
      AssetVariationDto variationDto =
          new AssetVariationDto(
              null,
              asset.getId(),
              AssetVariationTypeEnum.DELETED,
              asset.getQuantity(),
              null,
              asset.getAveragePrice(),
              null,
              occurredAt);
      variations.add(assetVariationMapper.create(variationDto, asset));
    }

    assetVariationRepository.saveAll(variations);
  }

  private void save(
      AssetEntity asset,
      AssetVariationTypeEnum changeType,
      BigDecimal previousQuantity,
      BigDecimal newQuantity,
      BigDecimal previousAveragePrice,
      BigDecimal newAveragePrice,
      LocalDateTime occurredAt) {
    AssetVariationDto variationDto =
        new AssetVariationDto(
            null,
            asset.getId(),
            changeType,
            previousQuantity,
            newQuantity,
            previousAveragePrice,
            newAveragePrice,
            occurredAt);
    AssetVariationEntity variation = assetVariationMapper.create(variationDto, asset);
    assetVariationRepository.save(variation);
  }
}
