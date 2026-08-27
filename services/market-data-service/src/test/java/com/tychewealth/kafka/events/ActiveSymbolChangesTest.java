package com.tychewealth.kafka.events;

import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
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
        new ActiveSymbolChanges(UUID.randomUUID(), Instant.now(), null, Set.of(SECOND_TEST_SYMBOL));

    assertTrue(changes.addedSymbols().isEmpty());
    assertEquals(Set.of(SECOND_TEST_SYMBOL), changes.removedSymbols());
  }

  @Test
  void constructorNormalizesNullRemovedSymbolsToEmptySet() {
    ActiveSymbolChanges changes =
        new ActiveSymbolChanges(UUID.randomUUID(), Instant.now(), Set.of(TEST_SYMBOL), null);

    assertEquals(Set.of(TEST_SYMBOL), changes.addedSymbols());
    assertTrue(changes.removedSymbols().isEmpty());
  }

  @Test
  void contractContainsEventMetadata() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    ActiveSymbolChanges changes =
        new ActiveSymbolChanges(
            eventId, occurredAt, Set.of(TEST_SYMBOL), Set.of(SECOND_TEST_SYMBOL));

    assertEquals(eventId, changes.eventId());
    assertEquals(occurredAt, changes.occurredAt());
  }
}
