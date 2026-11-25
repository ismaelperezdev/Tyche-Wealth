package com.tychewealth.controller.impl;

import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.exchangerate.CurrencyPairDto;
import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidExchangeRateQueryException;
import com.tychewealth.service.ExchangeRateReadService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExchangeRateApiControllerTest {

  @Test
  void rejectsCurrencyListsWithDifferentSizes() {
    ExchangeRateReadService exchangeRateReadService = mock(ExchangeRateReadService.class);
    ExchangeRateApiController controller = new ExchangeRateApiController(exchangeRateReadService);

    assertThatThrownBy(
            () ->
                controller.retrieveExchangeRates(
                    List.of(USD_CURRENCY, "JPY"), List.of(GBP_CURRENCY)))
        .isInstanceOfSatisfying(
            InvalidExchangeRateQueryException.class,
            exception ->
                assertThat(exception.getErrorDefinition())
                    .isEqualTo(ErrorDefinition.EXCHANGE_RATE_CURRENCY_PAIR_COUNT_MISMATCH));
    verifyNoInteractions(exchangeRateReadService);
  }

  @Test
  void forwardsCurrencyPairsInCorrespondingOrder() {
    ExchangeRateReadService exchangeRateReadService = mock(ExchangeRateReadService.class);
    ExchangeRateApiController controller = new ExchangeRateApiController(exchangeRateReadService);
    List<CurrencyPairDto> requestedPairs =
        List.of(
            new CurrencyPairDto(USD_CURRENCY, GBP_CURRENCY),
            new CurrencyPairDto(GBP_CURRENCY, USD_CURRENCY));
    List<ExchangeRateResponseDto> expectedRates =
        List.of(
            new ExchangeRateResponseDto(
                USD_CURRENCY,
                GBP_CURRENCY,
                new BigDecimal("0.78"),
                LocalDate.parse(EXCHANGE_RATE_TEST_DATE)),
            new ExchangeRateResponseDto(
                GBP_CURRENCY,
                USD_CURRENCY,
                new BigDecimal("1.28"),
                LocalDate.parse(EXCHANGE_RATE_TEST_DATE)));
    when(exchangeRateReadService.retrieveExchangeRates(requestedPairs)).thenReturn(expectedRates);

    var response =
        controller.retrieveExchangeRates(
            List.of(USD_CURRENCY, GBP_CURRENCY), List.of(GBP_CURRENCY, USD_CURRENCY));

    assertThat(response.getBody()).containsExactlyElementsOf(expectedRates);
    verify(exchangeRateReadService).retrieveExchangeRates(requestedPairs);
  }
}
