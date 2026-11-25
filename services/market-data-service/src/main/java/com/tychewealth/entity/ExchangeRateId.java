package com.tychewealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Composite identifier for an exchange rate observed on a date. */
@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class ExchangeRateId implements Serializable {

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  @Column(name = "quote_currency", nullable = false, length = 3)
  private String quoteCurrency;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  public ExchangeRateId(String baseCurrency, String quoteCurrency, LocalDate effectiveDate) {
    this.baseCurrency = baseCurrency;
    this.quoteCurrency = quoteCurrency;
    this.effectiveDate = effectiveDate;
  }
}
