package com.tychewealth.service;

import com.tychewealth.service.helper.MarketSymbolHelper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies active-symbol deltas to the MDS market symbol catalog. */
@Service
@RequiredArgsConstructor
public class MarketSymbolService {

  private final MarketSymbolHelper marketSymbolHelper;

  @Transactional
  public void applyChanges(Set<String> addedSymbols, Set<String> removedSymbols) {
    marketSymbolHelper.applyChanges(addedSymbols, removedSymbols);
  }
}
