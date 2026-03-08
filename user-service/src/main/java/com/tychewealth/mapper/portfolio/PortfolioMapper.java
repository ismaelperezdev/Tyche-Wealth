package com.tychewealth.mapper.portfolio;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.mapper.GenericMapper;
import com.tychewealth.mapper.GenericMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GenericMapperConfig.class)
public interface PortfolioMapper
    extends GenericMapper<
        PortfolioResponseDto,
        PortfolioEntity,
        PortfolioCreateRequestDto,
        PortfolioUpdateRequestDto> {}
