package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tychewealth.kafka.events.ActiveUsersEvent;
import com.tychewealth.service.activesymbol.ActiveSymbolService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveUsersEventConsumerTest {

  @Mock private ActiveSymbolService activeSymbolService;

  private ActiveUsersEventConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new ActiveUsersEventConsumer(activeSymbolService);
  }

  @Test
  void consumeDelegatesValidActiveUsersEvent() {
    ActiveUsersEvent event = new ActiveUsersEvent(Instant.now(), Set.of(TEST_USER_ID));

    assertDoesNotThrow(() -> consumer.consume(event, "active-users"));
    verify(activeSymbolService).synchronizeSymbols(Set.of(TEST_USER_ID));
  }

  @Test
  void consumeDelegatesEmptySymbolsWhenPayloadIsNull() {
    assertDoesNotThrow(() -> consumer.consume(null, "active-users"));
    verify(activeSymbolService).synchronizeSymbols(Set.of());
  }

  @Test
  void consumeDelegatesNullUserIdsAsNullForNow() {
    ActiveUsersEvent event = new ActiveUsersEvent(Instant.now(), null);

    assertDoesNotThrow(() -> consumer.consume(event, "active-users"));
    verify(activeSymbolService).synchronizeSymbols(null);
    verifyNoMoreInteractions(activeSymbolService);
  }
}
