package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CurrencyCodeEnum {
    EUR(1L, "EUR", "Euro"),
    USD(2L, "USD", "US Dollar"),
    GBP(3L, "GBP", "British Pound"),
    CHF(4L, "CHF", "Swiss Franc"),
    JPY(5L, "JPY", "Japanese Yen"),
    CNY(6L, "CNY", "Chinese Yuan"),
    ARS(7L, "ARS", "Argentine Peso"),
    BRL(9L, "BRL", "Brazilian Real"),
    CLP(10L, "CLP", "Chilean Peso"),
    COP(11L, "COP", "Colombian Peso"),
    DOP(14L, "DOP", "Dominican Peso"),
    MXN(17L, "MXN", "Mexican Peso"),
    PEN(20L, "PEN", "Peruvian Sol"),
    PYG(21L, "PYG", "Paraguayan Guarani"),
    SVC(22L, "SVC", "Salvadoran Colon"),
    UYU(23L, "UYU", "Uruguayan Peso"),
    VES(24L, "VES", "Venezuelan Bolivar");

    private final Long id;
    private final String abbreviation;
    private final String name;
}
