package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvestmentHorizonEnum {

    SHORT(1L, "SHORT"),
    MEDIUM(2L, "MEDIUM"),
    LONG(3L, "LONG");

    private final Long id;
    private final String name;
}
