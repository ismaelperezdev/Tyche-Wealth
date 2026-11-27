package com.tychewealth.dto.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Unrealized profit/loss and acquisition-cost amounts for one valuation timestamp. */
public record PortfolioValuationPointDto(
    Instant timestamp,
    BigDecimal unrealizedProfitLoss,
    BigDecimal acquisitionCost,
    List<PortfolioValuationPriceObservationDto> priceObservations) {

  public PortfolioValuationPointDto {
    priceObservations = List.copyOf(priceObservations);
  }
}
