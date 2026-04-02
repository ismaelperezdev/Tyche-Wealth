package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_GENERIC;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_60_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_00;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_LESS_THAN_OR_EQUAL_TO_1_00;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_1_INTEGER_DIGIT_AND_2_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_NULL;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class PortfolioTestData {

  private static final BigDecimal TEST_NEGATIVE_MAX_RISK = new BigDecimal("-0.01");
  private static final BigDecimal TEST_EXCESSIVE_MAX_RISK = new BigDecimal("1.01");
  private static final BigDecimal TEST_MAX_RISK_TOO_MANY_DECIMALS = new BigDecimal("0.123");

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
}
