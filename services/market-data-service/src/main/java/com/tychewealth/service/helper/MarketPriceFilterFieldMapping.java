package com.tychewealth.service.helper;

import static com.tychewealth.constants.CommonConstants.SYMBOL_QUERY_PARAM;
import static com.tychewealth.constants.MarketPriceFieldConstants.BUCKET_START;
import static com.tychewealth.constants.MarketPriceFieldConstants.CAPTURED_AT;
import static com.tychewealth.constants.MarketPriceFieldConstants.ID;
import static com.tychewealth.constants.MarketPriceFieldConstants.PRICE;

import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import com.tychewealth.listing.shared.FilterFieldMapping;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Maps public market-price filters to checkpoint persistence fields. */
@Component
public class MarketPriceFilterFieldMapping extends FilterFieldMapping {

  @Override
  protected Map<String, String> fieldNameMap() {
    return Map.of(
        SYMBOL_QUERY_PARAM,
        "marketSymbol." + SYMBOL_QUERY_PARAM,
        PRICE,
        PRICE,
        CAPTURED_AT,
        CAPTURED_AT,
        BUCKET_START,
        BUCKET_START,
        ID,
        ID);
  }

  @Override
  protected Map<String, Function<String, Object>> valueTransformers() {
    return Map.of(
        SYMBOL_QUERY_PARAM,
        value -> value.trim().toUpperCase(Locale.ROOT),
        CAPTURED_AT,
        this::toUtcLocalDateTime,
        BUCKET_START,
        this::toUtcLocalDateTime);
  }

  private LocalDateTime toUtcLocalDateTime(String value) {
    try {
      return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    } catch (DateTimeException exception) {
      throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID);
    }
  }
}
