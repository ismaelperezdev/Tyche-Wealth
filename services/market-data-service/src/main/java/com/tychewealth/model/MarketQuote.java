package com.tychewealth.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable market quote returned by a quote provider. */
public record MarketQuote(String symbol, BigDecimal price, LocalDateTime capturedAt) {}
