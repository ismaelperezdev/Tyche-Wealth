package com.tychewealth.entity;

import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "assets")
public class AssetEntity {

  @Id
  @SequenceGenerator(name = "assets_seq_gen", sequenceName = "assets_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assets_seq_gen")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  @NotNull(message = "Portfolio cannot be null")
  private PortfolioEntity portfolio;

  @Column(name = "symbol", nullable = false, length = 20)
  @NotBlank(message = "Symbol cannot be blank")
  @Size(max = 20, message = "Symbol must be at most 20 characters")
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(name = "asset_type", nullable = false, length = 30)
  @NotNull(message = "Asset type cannot be null")
  private AssetTypeEnum assetType;

  @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
  @NotNull(message = "Quantity cannot be null")
  @Digits(
      integer = 11,
      fraction = 8,
      message = "Quantity must have up to 11 integer digits and 8 decimals")
  @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
  private BigDecimal quantity;

  @Column(name = "average_price", nullable = false, precision = 19, scale = 4)
  @NotNull(message = "Average price cannot be null")
  @Digits(
      integer = 15,
      fraction = 4,
      message = "Average price must have up to 15 integer digits and 4 decimals")
  @DecimalMin(value = "0.0000", inclusive = false, message = "Average price must be greater than 0")
  private BigDecimal averagePrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false, length = 3)
  @NotNull(message = "Currency cannot be null")
  private CurrencyCodeEnum currency;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

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
