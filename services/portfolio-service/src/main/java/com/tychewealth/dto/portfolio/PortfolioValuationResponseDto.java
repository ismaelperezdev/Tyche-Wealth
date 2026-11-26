package com.tychewealth.dto.portfolio;

import com.tychewealth.enums.CurrencyCodeEnum;
import java.util.List;

/** Historical unrealized profit/loss series for a portfolio. */
public record PortfolioValuationResponseDto(
    CurrencyCodeEnum baseCurrency,
    List<PortfolioValuationPointDto> points,
    List<PortfolioValuationExchangeRateDto> fxRates) {

  public PortfolioValuationResponseDto {
    points = List.copyOf(points);
    fxRates = List.copyOf(fxRates);
  }
}
