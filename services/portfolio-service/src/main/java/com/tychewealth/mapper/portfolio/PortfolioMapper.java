package com.tychewealth.mapper.portfolio;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.PortfolioValuationExchangeRateDto;
import com.tychewealth.dto.portfolio.PortfolioValuationPointDto;
import com.tychewealth.dto.portfolio.PortfolioValuationPriceObservationDto;
import com.tychewealth.dto.portfolio.PortfolioValuationResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.mapper.GenericMapper;
import com.tychewealth.mapper.GenericMapperConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
        PortfolioUpdateRequestDto> {

  default Instant toInstant(AssetVariationEntity variation) {
    return variation.getOccurredAt().toInstant(ZoneOffset.UTC);
  }

  default PortfolioValuationPointDto toValuationPoint(
      Instant timestamp,
      BigDecimal unrealizedProfitLoss,
      BigDecimal acquisitionCost,
      List<PortfolioValuationPriceObservationDto> priceObservations) {
    return new PortfolioValuationPointDto(
        timestamp, unrealizedProfitLoss, acquisitionCost, priceObservations);
  }

  default PortfolioValuationResponseDto toValuationResponse(
      CurrencyCodeEnum baseCurrency,
      List<PortfolioValuationPointDto> points,
      List<PortfolioValuationExchangeRateDto> fxRates) {
    return new PortfolioValuationResponseDto(baseCurrency, points, fxRates);
  }
}
