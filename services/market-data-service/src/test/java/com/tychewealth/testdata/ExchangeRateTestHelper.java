package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.EUR_CURRENCY;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tychewealth.client.FrankfurterApiClient;
import com.tychewealth.config.properties.ExchangeRateIngestionProperties;
import com.tychewealth.entity.ExchangeRateEntity;
import com.tychewealth.entity.ExchangeRateId;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidExchangeRateQueryException;
import com.tychewealth.repository.ExchangeRateRepository;
import com.tychewealth.service.ExchangeRateIngestionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Shared factories and assertions for exchange-rate tests. */
public class ExchangeRateTestHelper {

  public static ExchangeRateEntity rate(String quoteCurrency, String effectiveDate) {
    return new ExchangeRateEntity(
        new ExchangeRateId(EUR_CURRENCY, quoteCurrency, LocalDate.parse(effectiveDate)),
        BigDecimal.ONE);
  }

  public static ExchangeRateEntity rate(String quoteCurrency, String effectiveDate, String value) {
    return new ExchangeRateEntity(
        new ExchangeRateId(EUR_CURRENCY, quoteCurrency, LocalDate.parse(effectiveDate)),
        new BigDecimal(value));
  }

  public static ExchangeRateIngestionService createIngestionService(
      FrankfurterApiClient frankfurterApiClient, ExchangeRateRepository exchangeRateRepository) {
    return new ExchangeRateIngestionService(
        frankfurterApiClient,
        exchangeRateRepository,
        new ExchangeRateIngestionProperties(EUR_CURRENCY, List.of(USD_CURRENCY, GBP_CURRENCY)));
  }

  public static void assertInvalidQuery(ThrowingCallable request, ErrorDefinition expectedError) {
    assertThatThrownBy(request)
        .isInstanceOfSatisfying(
            InvalidExchangeRateQueryException.class,
            exception -> assertThat(exception.getErrorDefinition()).isEqualTo(expectedError));
  }
}
