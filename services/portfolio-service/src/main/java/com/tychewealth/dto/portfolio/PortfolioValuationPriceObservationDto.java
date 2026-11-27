package com.tychewealth.dto.portfolio;

import java.time.Instant;

/** Market-price observation used for one symbol at a valuation point. */
public record PortfolioValuationPriceObservationDto(
    String symbol, PortfolioValuationPriceSource source, Instant capturedAt) {}
