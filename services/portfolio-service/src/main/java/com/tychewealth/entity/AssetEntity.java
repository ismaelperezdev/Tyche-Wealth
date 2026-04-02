package com.tychewealth.entity;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_20_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_0;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_NULL;

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
  @NotNull(message = MUST_NOT_BE_NULL)
  private PortfolioEntity portfolio;

  @Column(name = "symbol", nullable = false, length = 20)
  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(max = 20, message = MUST_BE_AT_MOST_20_CHARACTERS)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(name = "asset_type", nullable = false, length = 30)
  @NotNull(message = MUST_NOT_BE_NULL)
  private AssetTypeEnum assetType;

  @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
  @NotNull(message = MUST_NOT_BE_NULL)
  @Digits(integer = 11, fraction = 8, message = MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS)
  @DecimalMin(value = "0.00000001", message = MUST_BE_GREATER_THAN_0)
  private BigDecimal quantity;

  @Column(name = "average_price", nullable = false, precision = 19, scale = 4)
  @NotNull(message = MUST_NOT_BE_NULL)
  @Digits(integer = 15, fraction = 4, message = MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS)
  @DecimalMin(value = "0.0000", inclusive = false, message = MUST_BE_GREATER_THAN_0)
  private BigDecimal averagePrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false, length = 3)
  @NotNull(message = MUST_NOT_BE_NULL)
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
