package com.tychewealth.kafka.consumers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.tychewealth.kafka.events.ActiveSymbolChanges;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActiveSymbolChangesEventConsumerTest {

  private final ActiveSymbolChangesEventConsumer consumer = new ActiveSymbolChangesEventConsumer();

  @Test
  void consumeDoesNotThrowWhenEventContainsAddedAndRemovedSymbols() {
    ActiveSymbolChanges event =
        new ActiveSymbolChanges(UUID.randomUUID(), Instant.now(), Set.of("AAPL"), Set.of("MSFT"));

    assertDoesNotThrow(() -> consumer.consume(event, "active-symbol-changes"));
  }

  @Test
  void consumeDoesNotThrowWhenEventIsNull() {
    assertDoesNotThrow(() -> consumer.consume(null, "active-symbol-changes"));
  }
}
