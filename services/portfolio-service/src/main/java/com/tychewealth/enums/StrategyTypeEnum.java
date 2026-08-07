package com.tychewealth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the investment strategy selected for a portfolio.
 *
 * <p>Each strategy exposes a stable identifier, a client-facing label, and a short description used
 * to characterize the portfolio's intended approach.
 */
@Getter
@AllArgsConstructor
public enum StrategyTypeEnum {
  DIVIDEND(1L, "dividend", "acciones que pagan dividendos"),
  GROWTH(2L, "growth", "empresas de crecimiento"),
  VALUE(3L, "value", "empresas infravaloradas"),
  INDEX(4L, "index", "index investing (S&P500, MSCI…)"),
  ETF(5L, "etf", "ETFs diversificados"),
  CRYPTO(6L, "crypto", "portfolio cripto"),
  INCOME(7L, "income", "generar cashflow"),
  BALANCED(8L, "balanced", "mezcla diversificada"),
  SPECULATIVE(9L, "speculative", "alto riesgo");

  private final Long id;
  private final String label;
  private final String description;
}
