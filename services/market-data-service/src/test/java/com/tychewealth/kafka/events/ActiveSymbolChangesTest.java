package com.tychewealth.kafka.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActiveSymbolChangesTest {

  @Test
  void constructorNormalizesNullAddedSymbolsToEmptySet() {
    ActiveSymbolChanges changes =
        new ActiveSymbolChanges(UUID.randomUUID(), Instant.now(), null, Set.of("MSFT"));

    assertTrue(changes.addedSymbols().isEmpty());
    assertEquals(Set.of("MSFT"), changes.removedSymbols());
  }

  @Test
  void constructorNormalizesNullRemovedSymbolsToEmptySet() {
    ActiveSymbolChanges changes =
        new ActiveSymbolChanges(UUID.randomUUID(), Instant.now(), Set.of("AAPL"), null);

    assertEquals(Set.of("AAPL"), changes.addedSymbols());
    assertTrue(changes.removedSymbols().isEmpty());
  }

  @Test
  void contractContainsEventMetadata() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    ActiveSymbolChanges changes =
        new ActiveSymbolChanges(eventId, occurredAt, Set.of("AAPL"), Set.of("MSFT"));

    assertEquals(eventId, changes.eventId());
    assertEquals(occurredAt, changes.occurredAt());
  }
}
