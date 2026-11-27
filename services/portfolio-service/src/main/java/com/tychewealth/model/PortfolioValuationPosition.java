package com.tychewealth.model;

import com.tychewealth.enums.CurrencyCodeEnum;
import java.math.BigDecimal;

/** Asset holding and cost basis at a portfolio valuation timestamp. */
public record PortfolioValuationPosition(
    Long assetId,
    String symbol,
    CurrencyCodeEnum currency,
    BigDecimal quantity,
    BigDecimal averagePrice) {}
