package com.tychewealth.kafka.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ActiveSymbolChangesTest {

  @Test
  void constructorNormalizesNullAddedSymbolsToEmptySet() {
    ActiveSymbolChanges changes = new ActiveSymbolChanges(null, Set.of("MSFT"));

    assertTrue(changes.addedSymbols().isEmpty());
    assertEquals(Set.of("MSFT"), changes.removedSymbols());
  }

  @Test
  void constructorNormalizesNullRemovedSymbolsToEmptySet() {
    ActiveSymbolChanges changes = new ActiveSymbolChanges(Set.of("AAPL"), null);

    assertEquals(Set.of("AAPL"), changes.addedSymbols());
    assertTrue(changes.removedSymbols().isEmpty());
  }
}
