package com.tychewealth.kafka.events;

import java.util.Set;

public record ActiveSymbolChanges(Set<String> addedSymbols, Set<String> removedSymbols) {

  public ActiveSymbolChanges {
    addedSymbols = addedSymbols == null ? Set.of() : Set.copyOf(addedSymbols);
    removedSymbols = removedSymbols == null ? Set.of() : Set.copyOf(removedSymbols);
  }
}
