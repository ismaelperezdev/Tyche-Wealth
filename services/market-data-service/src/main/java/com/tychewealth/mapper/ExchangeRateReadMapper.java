package com.tychewealth.mapper;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps calculated exchange rates to internal API response DTOs. */
@Mapper(componentModel = "spring")
public interface ExchangeRateReadMapper {

  @Mapping(target = "baseCurrency", source = "pair.baseCurrency")
  @Mapping(target = "quoteCurrency", source = "pair.quoteCurrency")
  ExchangeRateResponseDto toResponseDto(
      CurrencyPairDto pair, BigDecimal rate, LocalDate effectiveDate);
}
