package com.tychewealth.provider;

import com.tychewealth.model.MarketQuote;

/** Source of current market quotes. */
public interface MarketQuoteProvider {

  MarketQuote fetchQuote(String symbol);
}
