package com.tychewealth.dto.portfolio.request;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_60_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdateRequestDto {

  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 3, max = 60, message = MUST_BE_BETWEEN_3_AND_60_CHARACTERS)
  private String name;

  private String description;

  private CurrencyCodeEnum baseCurrency;

  private RiskProfileEnum riskProfile;

  private InvestmentHorizonEnum investmentHorizon;

  private StrategyTypeEnum strategyType;

  @Digits(integer = 1, fraction = 2, message = MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS)
  @DecimalMin(value = "0.00", message = MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00)
  @DecimalMax(value = "1.00", message = MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00)
  private BigDecimal maxRisk;
}
