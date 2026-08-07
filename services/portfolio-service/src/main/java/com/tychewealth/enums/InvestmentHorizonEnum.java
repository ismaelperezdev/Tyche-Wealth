package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the intended time horizon for a portfolio's investments.
 *
 * <p>The available values distinguish short-, medium-, and long-term investment objectives.
 */
@Getter
@AllArgsConstructor
public enum InvestmentHorizonEnum {
  SHORT(1L, "SHORT"),
  MEDIUM(2L, "MEDIUM"),
  LONG(3L, "LONG");

  private final Long id;
  private final String name;
}
