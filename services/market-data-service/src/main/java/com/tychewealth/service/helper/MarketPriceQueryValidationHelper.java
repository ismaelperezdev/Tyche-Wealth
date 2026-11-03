package com.tychewealth.service.helper;

import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Validates and normalizes parameters for market-price read queries. */
@Component
public class MarketPriceQueryValidationHelper {

  public List<String> normalizeSymbols(List<String> requestedSymbols) {
    if (requestedSymbols == null || requestedSymbols.isEmpty()) {
      throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_SYMBOLS_REQUIRED);
    }

    LinkedHashSet<String> symbols = new LinkedHashSet<>();
    for (String requestedSymbol : requestedSymbols) {
      if (requestedSymbol == null || requestedSymbol.isBlank()) {
        throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_SYMBOL_BLANK);
      }

      String normalizedSymbol = requestedSymbol.trim().toUpperCase(Locale.ROOT);
      if (normalizedSymbol.length() > MarketSymbolEntity.MAX_SYMBOL_LENGTH) {
        throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_SYMBOL_TOO_LONG);
      }
      symbols.add(normalizedSymbol);
    }
    return List.copyOf(symbols);
  }

  public void validateTimeRange(Instant from, Instant to) {
    if (from == null || to == null || from.isAfter(to)) {
      throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_TIME_RANGE_INVALID);
    }
  }
}
