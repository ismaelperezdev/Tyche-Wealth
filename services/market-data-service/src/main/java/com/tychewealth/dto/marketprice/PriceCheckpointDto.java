package com.tychewealth.dto.marketprice;

import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.enums.FilterOperator;
import java.math.BigDecimal;
import java.time.Instant;

/** A persisted market-price checkpoint. Timestamps are represented in UTC. */
@Filterable
public record PriceCheckpointDto(
    @Filterable(operators = {FilterOperator.EQ, FilterOperator.IN}) String symbol,
    BigDecimal price,
    Instant capturedAt,
    Instant bucketStart) {}
