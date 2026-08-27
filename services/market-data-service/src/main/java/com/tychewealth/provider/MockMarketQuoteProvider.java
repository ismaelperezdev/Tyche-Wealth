package com.tychewealth.provider;

import com.tychewealth.model.MarketQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Generates synthetic quotes for local development and tests. */
@Component
public class MockMarketQuoteProvider implements MarketQuoteProvider {

  private static final double MIN_PRICE = 1.0;
  private static final double MAX_PRICE = 1_000.0;
  private static final double MIN_CHANGE_PERCENT = -0.01;
  private static final double MAX_CHANGE_PERCENT = 0.01;
  private static final int PRICE_SCALE = 8;

  private final ConcurrentMap<String, BigDecimal> pricesBySymbol = new ConcurrentHashMap<>();

  @Override
  public MarketQuote fetchQuote(String symbol) {
    BigDecimal price =
        pricesBySymbol.compute(
            symbol,
            (ignored, currentPrice) ->
                currentPrice == null ? initialPrice() : nextPrice(currentPrice));

    return new MarketQuote(symbol, price, LocalDateTime.now(ZoneOffset.UTC));
  }

  private BigDecimal initialPrice() {
    return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(MIN_PRICE, MAX_PRICE))
        .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal nextPrice(BigDecimal currentPrice) {
    double changePercent =
        ThreadLocalRandom.current().nextDouble(MIN_CHANGE_PERCENT, MAX_CHANGE_PERCENT);
    BigDecimal change = currentPrice.multiply(BigDecimal.valueOf(changePercent));
    BigDecimal nextPrice = currentPrice.add(change);

    return nextPrice.max(BigDecimal.valueOf(MIN_PRICE)).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
  }
}
