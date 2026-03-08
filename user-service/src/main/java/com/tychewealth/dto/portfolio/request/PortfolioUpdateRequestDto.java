package com.tychewealth.dto.portfolio.request;

import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PortfolioUpdateRequestDto {

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 3, max = 60, message = "Name must be between 3 and 60 characters")
  @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
  private String name;

  private String description;

  private CurrencyCodeEnum baseCurrency;

  private RiskProfileEnum riskProfile;

  private InvestmentHorizonEnum investmentHorizon;

  private StrategyTypeEnum strategyType;

  @Digits(
      integer = 1,
      fraction = 2,
      message = "Max risk must have up to 1 integer digit and 2 decimals")
  @DecimalMin(value = "0.00", message = "Max risk must be greater than or equal to 0.00")
  @DecimalMax(value = "1.00", message = "Max risk must be less than or equal to 1.00")
  private BigDecimal maxRisk;
}
