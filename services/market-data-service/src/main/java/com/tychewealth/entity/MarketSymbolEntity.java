package com.tychewealth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing a symbol in the MDS active market catalog. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "market_symbols")
public class MarketSymbolEntity {

  public static final int MAX_SYMBOL_LENGTH = 20;

  @Id
  @SequenceGenerator(
      name = "market_symbols_seq_gen",
      sequenceName = "market_symbols_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "market_symbols_seq_gen")
  private Long id;

  @Column(name = "symbol", nullable = false, unique = true, length = MAX_SYMBOL_LENGTH)
  private String symbol;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deactivated_at")
  private LocalDateTime deactivatedAt;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
