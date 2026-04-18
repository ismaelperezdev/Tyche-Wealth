package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_USD;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_MEDIUM;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_GENERIC;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_MEDIUM;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_BALANCED;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_60_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_NULL;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;

import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class PortfolioTestData {

  private static final BigDecimal TEST_NEGATIVE_MAX_RISK = new BigDecimal("-0.01");
  private static final BigDecimal TEST_EXCESSIVE_MAX_RISK = new BigDecimal("1.01");
  private static final BigDecimal TEST_MAX_RISK_TOO_MANY_DECIMALS = new BigDecimal("0.123");
  private static final String TEST_INVALID_ENUM_OPTION = "INVALID";

  private PortfolioTestData() {}

  public static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(
            createRequestBody(
                " ",
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                TEST_BASE_CURRENCY_EUR,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_PORTFOLIO_MAX_RISK),
            MUST_NOT_BE_BLANK),
        Arguments.of(
            createRequestBody(
                "AB",
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                TEST_BASE_CURRENCY_EUR,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_PORTFOLIO_MAX_RISK),
            MUST_BE_BETWEEN_3_AND_60_CHARACTERS),
        Arguments.of(
            createRequestBody(
                "A".repeat(61),
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                TEST_BASE_CURRENCY_EUR,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_PORTFOLIO_MAX_RISK),
            MUST_BE_BETWEEN_3_AND_60_CHARACTERS),
        Arguments.of(
            createRequestBody(
                TEST_PORTFOLIO_NAME_RETIREMENT,
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                null,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_PORTFOLIO_MAX_RISK),
            MUST_NOT_BE_NULL),
        Arguments.of(
            createRequestBody(
                TEST_PORTFOLIO_NAME_RETIREMENT,
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                TEST_BASE_CURRENCY_EUR,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_NEGATIVE_MAX_RISK),
            MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00),
        Arguments.of(
            createRequestBody(
                TEST_PORTFOLIO_NAME_RETIREMENT,
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                TEST_BASE_CURRENCY_EUR,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_EXCESSIVE_MAX_RISK),
            MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00),
        Arguments.of(
            createRequestBody(
                TEST_PORTFOLIO_NAME_RETIREMENT,
                TEST_PORTFOLIO_DESCRIPTION_GENERIC,
                TEST_BASE_CURRENCY_EUR,
                TEST_RISK_PROFILE_LOW,
                TEST_INVESTMENT_HORIZON_LONG,
                TEST_STRATEGY_TYPE_INCOME,
                TEST_MAX_RISK_TOO_MANY_DECIMALS),
            MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS));
  }

  public static String malformedCreateRequest() {
    return "{\"name\":\"Retirement\",\"baseCurrency\":\"EUR\"";
  }

  public static String invalidEnumCreateRequest() {
    return createRequestBody(
        TEST_PORTFOLIO_NAME_RETIREMENT, null, TEST_INVALID_ENUM_OPTION, null, null, null, null);
  }

  public static Stream<Arguments> invalidBeanValidationCases() {
    return Stream.of(
        Arguments.of(invalidCreate(dto -> dto.setName(" ")), "name", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidCreate(dto -> dto.setName("AB")), "name", MUST_BE_BETWEEN_3_AND_60_CHARACTERS),
        Arguments.of(
            invalidCreate(dto -> dto.setName("A".repeat(61))),
            "name",
            MUST_BE_BETWEEN_3_AND_60_CHARACTERS),
        Arguments.of(
            invalidCreate(dto -> dto.setBaseCurrency(null)), "baseCurrency", MUST_NOT_BE_NULL),
        Arguments.of(
            invalidCreate(dto -> dto.setMaxRisk(TEST_NEGATIVE_MAX_RISK)),
            "maxRisk",
            MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00),
        Arguments.of(
            invalidCreate(dto -> dto.setMaxRisk(TEST_EXCESSIVE_MAX_RISK)),
            "maxRisk",
            MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00),
        Arguments.of(
            invalidCreate(dto -> dto.setMaxRisk(TEST_MAX_RISK_TOO_MANY_DECIMALS)),
            "maxRisk",
            MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS),
        Arguments.of(invalidUpdate(dto -> dto.setName(" ")), "name", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidUpdate(dto -> dto.setName("AB")), "name", MUST_BE_BETWEEN_3_AND_60_CHARACTERS),
        Arguments.of(
            invalidUpdate(dto -> dto.setMaxRisk(TEST_NEGATIVE_MAX_RISK)),
            "maxRisk",
            MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00),
        Arguments.of(
            invalidUpdate(dto -> dto.setMaxRisk(TEST_EXCESSIVE_MAX_RISK)),
            "maxRisk",
            MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00),
        Arguments.of(
            invalidUpdate(dto -> dto.setMaxRisk(TEST_MAX_RISK_TOO_MANY_DECIMALS)),
            "maxRisk",
            MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS),
        Arguments.of(invalidEntity(entity -> entity.setUserId(null)), "userId", MUST_NOT_BE_NULL),
        Arguments.of(invalidEntity(entity -> entity.setName(" ")), "name", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidEntity(entity -> entity.setName("AB")),
            "name",
            MUST_BE_BETWEEN_3_AND_60_CHARACTERS),
        Arguments.of(
            invalidEntity(entity -> entity.setBaseCurrency(null)),
            "baseCurrency",
            MUST_NOT_BE_NULL),
        Arguments.of(
            invalidEntity(entity -> entity.setMaxRisk(TEST_NEGATIVE_MAX_RISK)),
            "maxRisk",
            MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00),
        Arguments.of(
            invalidEntity(entity -> entity.setMaxRisk(TEST_EXCESSIVE_MAX_RISK)),
            "maxRisk",
            MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00),
        Arguments.of(
            invalidEntity(entity -> entity.setMaxRisk(TEST_MAX_RISK_TOO_MANY_DECIMALS)),
            "maxRisk",
            MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS));
  }

  private static PortfolioCreateRequestDto validCreate() {
    return new PortfolioCreateRequestDto(
        TEST_PORTFOLIO_NAME_RETIREMENT,
        TEST_PORTFOLIO_DESCRIPTION_GENERIC,
        CurrencyCodeEnum.valueOf(TEST_BASE_CURRENCY_EUR),
        RiskProfileEnum.valueOf(TEST_RISK_PROFILE_LOW),
        InvestmentHorizonEnum.valueOf(TEST_INVESTMENT_HORIZON_LONG),
        StrategyTypeEnum.valueOf(TEST_STRATEGY_TYPE_INCOME),
        TEST_PORTFOLIO_MAX_RISK);
  }

  private static PortfolioUpdateRequestDto validUpdate() {
    return new PortfolioUpdateRequestDto(
        TEST_PORTFOLIO_NAME_RETIREMENT,
        TEST_PORTFOLIO_DESCRIPTION_GENERIC,
        CurrencyCodeEnum.valueOf(TEST_BASE_CURRENCY_USD),
        RiskProfileEnum.valueOf(TEST_RISK_PROFILE_MEDIUM),
        InvestmentHorizonEnum.valueOf(TEST_INVESTMENT_HORIZON_MEDIUM),
        StrategyTypeEnum.valueOf(TEST_STRATEGY_TYPE_BALANCED),
        TEST_PORTFOLIO_MAX_RISK);
  }

  private static PortfolioEntity validEntity() {
    PortfolioEntity entity = new PortfolioEntity();
    entity.setUserId(TEST_USER_ID);
    entity.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    entity.setDescription(TEST_PORTFOLIO_DESCRIPTION_GENERIC);
    entity.setBaseCurrency(CurrencyCodeEnum.valueOf(TEST_BASE_CURRENCY_EUR));
    entity.setRiskProfile(RiskProfileEnum.valueOf(TEST_RISK_PROFILE_LOW));
    entity.setInvestmentHorizon(InvestmentHorizonEnum.valueOf(TEST_INVESTMENT_HORIZON_LONG));
    entity.setStrategyType(StrategyTypeEnum.valueOf(TEST_STRATEGY_TYPE_INCOME));
    entity.setMaxRisk(TEST_PORTFOLIO_MAX_RISK);
    return entity;
  }

  private static PortfolioCreateRequestDto invalidCreate(
      Consumer<PortfolioCreateRequestDto> mutator) {
    return mutate(validCreate(), mutator);
  }

  private static PortfolioUpdateRequestDto invalidUpdate(
      Consumer<PortfolioUpdateRequestDto> mutator) {
    return mutate(validUpdate(), mutator);
  }

  private static PortfolioEntity invalidEntity(Consumer<PortfolioEntity> mutator) {
    return mutate(validEntity(), mutator);
  }

  private static <T> T mutate(T target, Consumer<T> mutator) {
    mutator.accept(target);
    return target;
  }
}
