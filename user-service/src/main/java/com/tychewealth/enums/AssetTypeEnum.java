package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssetTypeEnum {

    STOCK(1L, "STOCK", "Shares of publicly traded companies"),
    ETF(2L, "ETF", "Exchange Traded Funds tracking indexes or sectors"),
    CRYPTO(3L, "CRYPTO", "Cryptocurrencies traded on blockchain networks"),
    BOND(4L, "BOND", "Debt instruments issued by governments or corporations"),
    COMMODITY(5L, "COMMODITY", "Physical commodities such as gold, oil or agricultural products"),
    FOREX(6L, "FOREX", "Foreign exchange currency pairs");

    private final Long id;
    private final String name;
    private final String description;
}
