package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the target risk tolerance of a portfolio.
 *
 * <p>The profile is used to describe whether the portfolio is intended for low, medium, or high
 * risk exposure.
 */
@Getter
@AllArgsConstructor
public enum RiskProfileEnum {
  LOW(1L, "LOW"),
  MEDIUM(2L, "MEDIUM"),
  HIGH(3L, "HIGH");

  private final Long id;
  private final String name;
}
