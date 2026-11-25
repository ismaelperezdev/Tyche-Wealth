package com.tychewealth.service.helper;

import static com.tychewealth.constants.TestConstants.EUR_CURRENCY;
import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.TEST_EXCHANGE_RATE;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static com.tychewealth.testdata.ExchangeRateTestHelper.rate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExchangeRateReadHelperTest {

  private final ExchangeRateReadHelper helper =
      new ExchangeRateReadHelper(mock(ExchangeRateRepository.class));

  @Test
  void buildsNormalizedDistinctPairsInRequestOrder() {
    List<CurrencyPairDto> pairs =
        helper.toCurrencyPairs(
            List.of(" eur ", USD_CURRENCY, EUR_CURRENCY),
            List.of("usd", GBP_CURRENCY, USD_CURRENCY));

    assertThat(pairs)
        .containsExactly(
            new CurrencyPairDto(EUR_CURRENCY, USD_CURRENCY),
            new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY));
  }

  @Test
  void calculatesCrossRateFromEurPivotObservations() {
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, TEST_EXCHANGE_RATE);
    ExchangeRateEntity eurToGbp = rate(GBP_CURRENCY, EXCHANGE_RATE_TEST_DATE, "0.85");

    var crossRate =
        helper.calculateCrossRate(
            new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY), eurToUsd, eurToGbp);

    assertThat(crossRate)
        .isEqualByComparingTo(
            new BigDecimal("0.85")
                .divide(new BigDecimal(TEST_EXCHANGE_RATE), java.math.MathContext.DECIMAL128));
  }
}
