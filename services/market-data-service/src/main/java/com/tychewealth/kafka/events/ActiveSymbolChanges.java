package com.tychewealth.kafka.events;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Carries the symbols added to and removed from the active market-symbol set. */
public record ActiveSymbolChanges(
    UUID eventId, Instant occurredAt, Set<String> addedSymbols, Set<String> removedSymbols) {

  public ActiveSymbolChanges {
    addedSymbols = addedSymbols == null ? Set.of() : Set.copyOf(addedSymbols);
    removedSymbols = removedSymbols == null ? Set.of() : Set.copyOf(removedSymbols);
  }
}
