package com.tychewealth.entity;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_60_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_NULL;

import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "portfolios")
public class PortfolioEntity {

  @Id
  @SequenceGenerator(
      name = "portfolios_seq_gen",
      sequenceName = "portfolios_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "portfolios_seq_gen")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @NotNull(message = MUST_NOT_BE_NULL)
  private UserEntity user;

  @OneToMany(mappedBy = "portfolio")
  private List<AssetEntity> assets = new ArrayList<>();

  @Column(name = "name", nullable = false, length = 60)
  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 3, max = 60, message = MUST_BE_BETWEEN_3_AND_60_CHARACTERS)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_currency", nullable = false, length = 3)
  @NotNull(message = MUST_NOT_BE_NULL)
  private CurrencyCodeEnum baseCurrency;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_profile", length = 50)
  private RiskProfileEnum riskProfile;

  @Enumerated(EnumType.STRING)
  @Column(name = "investment_horizon", length = 50)
  private InvestmentHorizonEnum investmentHorizon;

  @Enumerated(EnumType.STRING)
  @Column(name = "strategy_type", length = 50)
  private StrategyTypeEnum strategyType;

  @Column(name = "max_risk", precision = 3, scale = 2)
  @Digits(integer = 1, fraction = 2, message = MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS)
  @DecimalMin(value = "0.00", message = MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00)
  @DecimalMax(value = "1.00", message = MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00)
  private BigDecimal maxRisk;

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
