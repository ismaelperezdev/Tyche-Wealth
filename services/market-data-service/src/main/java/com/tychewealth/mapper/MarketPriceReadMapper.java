package com.tychewealth.mapper;

import static com.tychewealth.constants.MarketPriceFieldConstants.BUCKET_START;
import static com.tychewealth.constants.MarketPriceFieldConstants.CAPTURED_AT;

import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.model.MarketQuote;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Maps market-price read models to API response DTOs. */
@Mapper(componentModel = "spring")
public interface MarketPriceReadMapper {

  @Mapping(target = CAPTURED_AT, source = CAPTURED_AT, qualifiedByName = "toUtcInstant")
  LatestPriceResponseDto toLatestPriceResponse(MarketQuote quote);

  @Mapping(target = "symbol", source = "marketSymbol.symbol")
  @Mapping(target = CAPTURED_AT, source = CAPTURED_AT, qualifiedByName = "toUtcInstant")
  @Mapping(target = BUCKET_START, source = BUCKET_START, qualifiedByName = "toUtcInstant")
  PriceCheckpointDto toPriceCheckpointDto(MarketPriceCheckpointEntity checkpoint);

  @Named("toUtcInstant")
  default Instant toUtcInstant(LocalDateTime timestamp) {
    return timestamp.toInstant(ZoneOffset.UTC);
  }
}
