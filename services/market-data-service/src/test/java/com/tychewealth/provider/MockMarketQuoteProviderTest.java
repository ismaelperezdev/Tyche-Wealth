package com.tychewealth.provider;

import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.model.MarketQuote;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MockMarketQuoteProviderTest {

  private static final BigDecimal MAX_CHANGE_PERCENT = new BigDecimal("0.0100001");

  private final MockMarketQuoteProvider provider = new MockMarketQuoteProvider();

  @Test
  void shouldKeepSubsequentPricesForSymbolWithinRealisticRange() {
    MarketQuote previousQuote = provider.fetchQuote(TEST_SYMBOL);

    for (int index = 0; index < 100; index++) {
      MarketQuote currentQuote = provider.fetchQuote(TEST_SYMBOL);
      BigDecimal relativeChange =
          currentQuote
              .price()
              .subtract(previousQuote.price())
              .abs()
              .divide(previousQuote.price(), 10, java.math.RoundingMode.HALF_UP);

      assertThat(currentQuote.symbol()).isEqualTo(TEST_SYMBOL);
      assertThat(currentQuote.price()).isPositive();
      assertThat(relativeChange).isLessThanOrEqualTo(MAX_CHANGE_PERCENT);
      previousQuote = currentQuote;
    }
  }
}
