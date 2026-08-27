package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.TestConstants.ACTIVE_SYMBOL_CHANGES_TOPIC;
import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tychewealth.kafka.events.ActiveSymbolChanges;
import com.tychewealth.service.MarketSymbolService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActiveSymbolChangesEventConsumerTest {

  private final MarketSymbolService marketSymbolService = mock(MarketSymbolService.class);
  private final ActiveSymbolChangesEventConsumer consumer =
      new ActiveSymbolChangesEventConsumer(marketSymbolService);

  @Test
  void consumeDelegatesAddedAndRemovedSymbolsToService() {
    ActiveSymbolChanges event =
        new ActiveSymbolChanges(
            UUID.randomUUID(), Instant.now(), Set.of(TEST_SYMBOL), Set.of(SECOND_TEST_SYMBOL));

    assertDoesNotThrow(() -> consumer.consume(event, ACTIVE_SYMBOL_CHANGES_TOPIC));

    verify(marketSymbolService).applyChanges(Set.of(TEST_SYMBOL), Set.of(SECOND_TEST_SYMBOL));
  }

  @Test
  void consumeIgnoresNullEvent() {
    assertDoesNotThrow(() -> consumer.consume(null, ACTIVE_SYMBOL_CHANGES_TOPIC));

    verifyNoInteractions(marketSymbolService);
  }
}
