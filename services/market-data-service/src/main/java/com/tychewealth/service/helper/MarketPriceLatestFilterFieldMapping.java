package com.tychewealth.service.helper;

import static com.tychewealth.constants.CommonConstants.SYMBOL_QUERY_PARAM;

import com.tychewealth.listing.shared.FilterFieldMapping;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maps latest-price listing fields to their lookup fields. */
@Component
public class MarketPriceLatestFilterFieldMapping extends FilterFieldMapping {

  @Override
  protected Map<String, String> fieldNameMap() {
    return Map.of(SYMBOL_QUERY_PARAM, SYMBOL_QUERY_PARAM);
  }
}
