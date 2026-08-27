package com.tychewealth.error.exception;

/** Raised when an active-symbol event contains an invalid market symbol. */
public class InvalidMarketSymbolException extends RuntimeException {

  public InvalidMarketSymbolException(String symbol, int maxLength) {
    super("Market symbol exceeds the maximum length of " + maxLength + " characters: " + symbol);
  }
}
