package com.tychewealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** JPA entity representing an immutable market-price checkpoint. */
@Entity
@Getter
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "market_price_checkpoints")
public class MarketPriceCheckpointEntity {

  @Id
  @SequenceGenerator(
      name = "market_price_checkpoints_seq_gen",
      sequenceName = "market_price_checkpoints_seq",
      allocationSize = 1)
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "market_price_checkpoints_seq_gen")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "market_symbol_id", nullable = false)
  private MarketSymbolEntity marketSymbol;

  @Column(name = "price", nullable = false, precision = 19, scale = 8)
  private BigDecimal price;

  @Column(name = "captured_at", nullable = false)
  private LocalDateTime capturedAt;

  @Column(name = "bucket_start", nullable = false)
  private LocalDateTime bucketStart;

  public MarketPriceCheckpointEntity(
      MarketSymbolEntity marketSymbol,
      BigDecimal price,
      LocalDateTime capturedAt,
      LocalDateTime bucketStart) {
    this.marketSymbol = marketSymbol;
    this.price = price;
    this.capturedAt = capturedAt;
    this.bucketStart = bucketStart;
  }
}
