package com.tychewealth.mapper.asset;

import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.mapper.GenericMapper;
import com.tychewealth.mapper.GenericMapperConfig;
import org.mapstruct.Mapper;

/**
 * Maps asset creation and update requests to {@link AssetEntity} and exposes asset responses as
 * {@link AssetResponseDto} instances.
 *
 * <p>Inherits the shared conversion contract from {@link GenericMapper} and applies the common
 * MapStruct configuration defined by {@link GenericMapperConfig}.
 */
@Mapper(config = GenericMapperConfig.class)
public interface AssetMapper
    extends GenericMapper<
        AssetResponseDto, AssetEntity, AssetCreateRequestDto, AssetUpdateRequestDto> {}
