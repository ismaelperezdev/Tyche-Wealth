package com.tychewealth.marketdataclient.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Exchange rate returned by the internal market-data API. */
public record ExchangeRate(
    String baseCurrency, String quoteCurrency, BigDecimal rate, LocalDate effectiveDate) {}
