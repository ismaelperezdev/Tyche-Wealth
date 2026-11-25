package com.tychewealth.service.helper;

import static com.tychewealth.constants.TestConstants.EUR_CURRENCY;
import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.entity.ExchangeRateId;
import com.tychewealth.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExchangeRateReadHelperTest {

  private final ExchangeRateReadHelper helper =
      new ExchangeRateReadHelper(mock(ExchangeRateRepository.class));

  @Test
  void buildsNormalizedDistinctPairsInRequestOrder() {
    List<CurrencyPairDto> pairs =
        helper.toCurrencyPairs(List.of(" eur ", "USD", "EUR"), List.of("usd", "GBP", "USD"));

    assertThat(pairs)
        .containsExactly(
            new CurrencyPairDto(EUR_CURRENCY, USD_CURRENCY),
            new CurrencyPairDto(USD_CURRENCY, "GBP"));
  }

  @Test
  void calculatesCrossRateFromEurPivotObservations() {
    ExchangeRateEntity eurToUsd = rate(USD_CURRENCY, EXCHANGE_RATE_TEST_DATE, "1.10");
    ExchangeRateEntity eurToGbp = rate("GBP", EXCHANGE_RATE_TEST_DATE, "0.85");

    var crossRate =
        helper.calculateCrossRate(new CurrencyPairDto(USD_CURRENCY, "GBP"), eurToUsd, eurToGbp);

    assertThat(crossRate)
        .isEqualByComparingTo(
            new BigDecimal("0.85")
                .divide(new BigDecimal("1.10"), java.math.MathContext.DECIMAL128));
  }

  private ExchangeRateEntity rate(String quoteCurrency, String effectiveDate, String value) {
    return new ExchangeRateEntity(
        new ExchangeRateId(EUR_CURRENCY, quoteCurrency, LocalDate.parse(effectiveDate)),
        new BigDecimal(value));
  }
}
