package com.tychewealth.constants;

import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.math.BigDecimal;

public final class TestConstants {

  public static final String TEST_JWT_SECRET = "4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=";
  public static final String TEST_EMAIL_LAURA = "laura.gomez@tychewealth.com";
  public static final String TEST_EMAIL_VALID = "valid@tychewealth.com";
  public static final String TEST_USERNAME_LAURA = "lauragomez";
  public static final String TEST_USERNAME_VALID = "validuser";
  public static final String TEST_PASSWORD_VALID = "Secret123!";
  public static final long TEST_USER_ID = 42L;
  public static final long TEST_OTHER_USER_ID = 84L;
  public static final String TEST_PORTFOLIO_NAME_RETIREMENT = "Retirement";
  public static final String TEST_PORTFOLIO_DESCRIPTION_LONG_TERM = "Long term portfolio";
  public static final String TEST_PORTFOLIO_DESCRIPTION_ANOTHER = "Another description";
  public static final String TEST_PORTFOLIO_DESCRIPTION_OTHER_OWNER = "Other owner";
  public static final String TEST_PORTFOLIO_DESCRIPTION_GENERIC = "desc";
  public static final BigDecimal TEST_PORTFOLIO_MAX_RISK = new BigDecimal("0.40");
  public static final BigDecimal TEST_PORTFOLIO_OTHER_MAX_RISK = new BigDecimal("0.30");
  public static final String TEST_BASE_CURRENCY_EUR = CurrencyCodeEnum.EUR.name();
  public static final String TEST_BASE_CURRENCY_USD = CurrencyCodeEnum.USD.name();
  public static final String TEST_RISK_PROFILE_LOW = RiskProfileEnum.LOW.name();
  public static final String TEST_RISK_PROFILE_MEDIUM = RiskProfileEnum.MEDIUM.name();
  public static final String TEST_INVESTMENT_HORIZON_LONG = InvestmentHorizonEnum.LONG.name();
  public static final String TEST_INVESTMENT_HORIZON_MEDIUM = InvestmentHorizonEnum.MEDIUM.name();
  public static final String TEST_STRATEGY_TYPE_INCOME = StrategyTypeEnum.INCOME.name();
  public static final String TEST_STRATEGY_TYPE_BALANCED = StrategyTypeEnum.BALANCED.name();

  private TestConstants() {}
}
