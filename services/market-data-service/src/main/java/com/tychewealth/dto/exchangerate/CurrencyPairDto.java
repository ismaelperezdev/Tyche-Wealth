package com.tychewealth.dto.exchangerate;

/** A requested exchange-rate currency pair. */
public record CurrencyPairDto(String baseCurrency, String quoteCurrency) {}
