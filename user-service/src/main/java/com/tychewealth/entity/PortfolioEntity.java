package com.tychewealth.entity;

import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "portfolios_seq_gen")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User cannot be null")
    private UserEntity user;

    @OneToMany(mappedBy = "portfolio")
    private List<AssetEntity> assets = new ArrayList<>();

    @Column(name = "name", nullable = false, length = 60)
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 3, max = 60, message = "Name must be between 3 and 60 characters")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false, length = 3)
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
    @Digits(integer = 1, fraction = 2, message = "Max risk must have up to 1 integer digits and 2 decimals")
    @DecimalMin(value = "0.00", message = "Max risk must be greater than or equal to 0.00")
    @DecimalMax(value = "1.00", message = "Max risk must be less than or equal to 1.00")
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
