package com.tychewealth.kafka.publishers;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.error.exception.EventPublishingException;
import com.tychewealth.kafka.events.ActiveUsersEvent;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActiveUsersEventPublisherTest {

  @Mock private KafkaTemplate<String, ActiveUsersEvent> kafkaTemplate;

  private ActiveUsersEventPublisher activeUsersEventPublisher;

  @BeforeEach
  void setUp() {
    activeUsersEventPublisher = new ActiveUsersEventPublisher(kafkaTemplate);
    ReflectionTestUtils.setField(activeUsersEventPublisher, "activeUsersTopic", "active-users");
    ReflectionTestUtils.setField(activeUsersEventPublisher, "publishTimeoutMs", 1_000L);
  }

  @Test
  void publishLogsSuccessOnlyAfterBrokerAcknowledgement() {
    ActiveUsersEvent event = new ActiveUsersEvent(Instant.now(), Set.of(TEST_USER_ID));
    CompletableFuture<SendResult<String, ActiveUsersEvent>> sendFuture =
        CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send("active-users", event)).thenReturn(sendFuture);

    assertDoesNotThrow(() -> activeUsersEventPublisher.publish(event));

    verify(kafkaTemplate).send("active-users", event);
  }

  @Test
  void publishWrapsDeliveryFailuresWithEventContext() {
    ActiveUsersEvent event = new ActiveUsersEvent(Instant.now(), Set.of(TEST_USER_ID));
    CompletableFuture<SendResult<String, ActiveUsersEvent>> sendFuture = new CompletableFuture<>();
    sendFuture.completeExceptionally(new IllegalStateException("broker unavailable"));
    when(kafkaTemplate.send("active-users", event)).thenReturn(sendFuture);

    EventPublishingException exception =
        assertThrows(
            EventPublishingException.class, () -> activeUsersEventPublisher.publish(event));

    assertEquals("active-users", exception.getMetadata().get("topic"));
    assertEquals("1", exception.getMetadata().get("activeUsers"));
    verify(kafkaTemplate).send("active-users", event);
  }
}
