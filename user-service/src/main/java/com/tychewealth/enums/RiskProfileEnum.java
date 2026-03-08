package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RiskProfileEnum {
  LOW(1L, "LOW"),
  MEDIUM(2L, "MEDIUM"),
  HIGH(3L, "HIGH");

  private final Long id;
  private final String name;
}
