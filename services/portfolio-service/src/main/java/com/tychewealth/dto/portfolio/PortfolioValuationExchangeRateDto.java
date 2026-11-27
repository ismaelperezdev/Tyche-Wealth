package com.tychewealth.dto.portfolio;

import java.time.LocalDate;

/** Effective date of an exchange rate used in a portfolio valuation. */
public record PortfolioValuationExchangeRateDto(
    String baseCurrency, String quoteCurrency, LocalDate effectiveDate) {}
