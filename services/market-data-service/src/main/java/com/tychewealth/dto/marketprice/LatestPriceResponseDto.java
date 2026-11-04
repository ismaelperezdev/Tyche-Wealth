package com.tychewealth.dto.marketprice;

import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.enums.FilterOperator;
import java.math.BigDecimal;
import java.time.Instant;

/** Latest available market price for a symbol. */
public record LatestPriceResponseDto(
    @Filterable(operators = {FilterOperator.EQ, FilterOperator.IN}) String symbol,
    BigDecimal price,
    Instant capturedAt) {}
