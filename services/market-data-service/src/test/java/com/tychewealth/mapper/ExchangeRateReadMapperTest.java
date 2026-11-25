package com.tychewealth.mapper;

import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ExchangeRateReadMapperTest {

  private final ExchangeRateReadMapper mapper = Mappers.getMapper(ExchangeRateReadMapper.class);

  @Test
  void mapsCurrencyPairAndCalculatedRateToResponse() {
    CurrencyPairDto pair = new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY);
    BigDecimal rate = new BigDecimal("0.7727272727");
    LocalDate effectiveDate = LocalDate.parse(EXCHANGE_RATE_TEST_DATE);

    ExchangeRateResponseDto response = mapper.toResponseDto(pair, rate, effectiveDate);

    assertThat(response)
        .isEqualTo(new ExchangeRateResponseDto(USD_CURRENCY, GBP_CURRENCY, rate, effectiveDate));
  }
}
