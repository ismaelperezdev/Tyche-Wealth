package com.tychewealth.mapper.portfolio;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.mapper.GenericMapper;
import com.tychewealth.mapper.GenericMapperConfig;
import org.mapstruct.Mapper;

/**
 * Maps portfolio creation and update requests to {@link PortfolioEntity} and exposes portfolio
 * responses as {@link PortfolioResponseDto} instances.
 *
 * <p>Inherits the shared conversion contract from {@link GenericMapper} and applies the common
 * MapStruct configuration defined by {@link GenericMapperConfig}.
 */
@Mapper(config = GenericMapperConfig.class)
public interface PortfolioMapper
    extends GenericMapper<
        PortfolioResponseDto,
        PortfolioEntity,
        PortfolioCreateRequestDto,
        PortfolioUpdateRequestDto> {}
