package com.tychewealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** JPA entity representing an exchange rate for a currency pair and effective date. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exchange_rates")
public class ExchangeRateEntity {

  @EmbeddedId private ExchangeRateId id;

  @Column(name = "rate", nullable = false, precision = 24, scale = 12)
  private BigDecimal rate;

  public ExchangeRateEntity(ExchangeRateId id, BigDecimal rate) {
    this.id = id;
    this.rate = rate;
  }
}
