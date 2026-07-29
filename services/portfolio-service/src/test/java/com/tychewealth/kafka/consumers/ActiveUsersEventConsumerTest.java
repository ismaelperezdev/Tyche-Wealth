package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.tychewealth.kafka.events.ActiveUsersEvent;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActiveUsersEventConsumerTest {

  private ActiveUsersEventConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new ActiveUsersEventConsumer();
  }

  @Test
  void consumeAcceptsValidActiveUsersEvent() {
    ActiveUsersEvent event = new ActiveUsersEvent(Instant.now(), Set.of(TEST_USER_ID));

    assertDoesNotThrow(() -> consumer.consume(event, "active-users"));
  }

  @Test
  void consumeAcceptsNullEventPayload() {
    assertDoesNotThrow(() -> consumer.consume(null, "active-users"));
  }

  @Test
  void consumeAcceptsEventWithNullUserIds() {
    ActiveUsersEvent event = new ActiveUsersEvent(Instant.now(), null);

    assertDoesNotThrow(() -> consumer.consume(event, "active-users"));
  }
}
