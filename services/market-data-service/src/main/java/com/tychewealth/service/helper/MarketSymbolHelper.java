package com.tychewealth.service.helper;

import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.error.exception.InvalidMarketSymbolException;
import com.tychewealth.repository.MarketSymbolRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Contains the symbol normalization and persistence workflow for active-symbol deltas. */
@Component
@RequiredArgsConstructor
public class MarketSymbolHelper {

  private final MarketSymbolRepository marketSymbolRepository;

  @Value("${app.market-symbol.batch-size:500}")
  private int batchSize = 500;

  @PostConstruct
  void validateBatchSize() {
    if (batchSize <= 0) {
      throw new IllegalStateException("app.market-symbol.batch-size must be greater than zero");
    }
  }

  public void applyChanges(Set<String> addedSymbols, Set<String> removedSymbols) {
    Set<String> normalizedAddedSymbols = normalizeSymbols(addedSymbols);
    Set<String> normalizedRemovedSymbols = normalizeSymbols(removedSymbols);
    normalizedAddedSymbols.removeAll(normalizedRemovedSymbols);

    applyActivations(normalizedAddedSymbols);
    applyDeactivations(normalizedRemovedSymbols);
  }

  private void applyActivations(Set<String> symbols) {
    for (List<String> batch : batches(symbols)) {
      marketSymbolRepository.insertIfAbsentBatch(batch.toArray(String[]::new), true, null);
      marketSymbolRepository.updateActiveStateBatch(batch, true, null);
    }
  }

  private void applyDeactivations(Set<String> symbols) {
    LocalDateTime deactivatedAt = LocalDateTime.now();
    for (List<String> batch : batches(symbols)) {
      marketSymbolRepository.insertIfAbsentBatch(
          batch.toArray(String[]::new), false, deactivatedAt);
      marketSymbolRepository.updateActiveStateBatch(batch, false, deactivatedAt);
    }
  }

  private List<List<String>> batches(Set<String> symbols) {
    List<String> symbolList = List.copyOf(symbols);
    List<List<String>> batches = new ArrayList<>();
    for (int start = 0; start < symbolList.size(); start += batchSize) {
      batches.add(symbolList.subList(start, Math.min(start + batchSize, symbolList.size())));
    }
    return batches;
  }

  private Set<String> normalizeSymbols(Set<String> symbols) {
    Set<String> normalizedSymbols = new LinkedHashSet<>();
    if (symbols == null) {
      return normalizedSymbols;
    }

    for (String symbol : symbols) {
      String normalizedSymbol = normalize(symbol);
      if (normalizedSymbol != null) {
        normalizedSymbols.add(normalizedSymbol);
      }
    }

    return normalizedSymbols;
  }

  private String normalize(String symbol) {
    if (symbol == null) {
      return null;
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    if (normalizedSymbol.isEmpty()) {
      return null;
    }
    if (normalizedSymbol.length() > MarketSymbolEntity.MAX_SYMBOL_LENGTH) {
      throw new InvalidMarketSymbolException(symbol, MarketSymbolEntity.MAX_SYMBOL_LENGTH);
    }
    return normalizedSymbol;
  }
}
