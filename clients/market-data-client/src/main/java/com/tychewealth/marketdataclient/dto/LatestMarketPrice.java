package com.tychewealth.marketdataclient.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Latest available market price for a symbol. */
public record LatestMarketPrice(String symbol, BigDecimal price, Instant capturedAt) {}
