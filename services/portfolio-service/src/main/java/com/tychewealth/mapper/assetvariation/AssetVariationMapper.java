package com.tychewealth.mapper.assetvariation;

import com.tychewealth.dto.asset.AssetVariationDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.mapper.GenericMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps asset variation data between internal DTOs and persistence entities. */
@Mapper(config = GenericMapperConfig.class)
public interface AssetVariationMapper {

  @Mapping(target = "assetId", source = "asset.id")
  AssetVariationDto toDto(AssetVariationEntity entity);

  @Mapping(target = "id", ignore = true)
  AssetVariationEntity toEntityForCreation(AssetVariationDto dto, AssetEntity asset);
}
