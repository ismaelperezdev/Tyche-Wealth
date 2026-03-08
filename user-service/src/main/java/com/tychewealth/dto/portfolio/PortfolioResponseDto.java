package com.tychewealth.dto.portfolio;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioResponseDto {

  private Long id;
  private String name;
  private String description;
  private CurrencyCodeEnum baseCurrency;
  private RiskProfileEnum riskProfile;
  private InvestmentHorizonEnum investmentHorizon;
  private StrategyTypeEnum strategyType;
  private BigDecimal maxRisk;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
