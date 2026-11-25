package com.tychewealth.controller.impl;

import static com.tychewealth.constants.TestConstants.EXCHANGE_RATE_TEST_DATE;
import static com.tychewealth.constants.TestConstants.GBP_CURRENCY;
import static com.tychewealth.constants.TestConstants.USD_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.exchangerate.ExchangeRateResponseDto;
import com.tychewealth.service.ExchangeRateReadService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExchangeRateApiControllerTest {

  @Test
  void forwardsCurrencyListsToReadService() {
    ExchangeRateReadService exchangeRateReadService = mock(ExchangeRateReadService.class);
    ExchangeRateApiController controller = new ExchangeRateApiController(exchangeRateReadService);
    List<String> baseCurrencies = List.of(USD_CURRENCY, GBP_CURRENCY);
    List<String> quoteCurrencies = List.of(GBP_CURRENCY, USD_CURRENCY);
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
    when(exchangeRateReadService.retrieveExchangeRates(baseCurrencies, quoteCurrencies))
        .thenReturn(expectedRates);

    var response = controller.retrieveExchangeRates(baseCurrencies, quoteCurrencies);

    assertThat(response.getBody()).containsExactlyElementsOf(expectedRates);
    verify(exchangeRateReadService).retrieveExchangeRates(baseCurrencies, quoteCurrencies);
  }
}
