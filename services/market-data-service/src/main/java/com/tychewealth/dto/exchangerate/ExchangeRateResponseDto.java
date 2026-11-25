package com.tychewealth.dto.exchangerate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An exchange rate and the date on which it became effective. */
public record ExchangeRateResponseDto(
    String baseCurrency, String quoteCurrency, BigDecimal rate, LocalDate effectiveDate) {}
