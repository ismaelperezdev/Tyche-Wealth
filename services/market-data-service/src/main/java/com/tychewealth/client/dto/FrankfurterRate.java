package com.tychewealth.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Exchange-rate record returned by Frankfurter's rates endpoint. */
public record FrankfurterRate(LocalDate date, String base, String quote, BigDecimal rate) {}
