package com.tychewealth.kafka.publishers;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.error.exception.EventPublishingException;
import com.tychewealth.kafka.events.ActiveSymbolChanges;
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
class ActiveSymbolChangesEventPublisherTest {

  private static final String ACTIVE_SYMBOL_CHANGES_TOPIC = "active-symbol-changes";

  @Mock private KafkaTemplate<String, ActiveSymbolChanges> kafkaTemplate;

  private ActiveSymbolChangesEventPublisher activeSymbolChangesEventPublisher;

  @BeforeEach
  void setUp() {
    activeSymbolChangesEventPublisher = new ActiveSymbolChangesEventPublisher(kafkaTemplate);
    ReflectionTestUtils.setField(
        activeSymbolChangesEventPublisher, "activeSymbolChangesTopic", ACTIVE_SYMBOL_CHANGES_TOPIC);
    ReflectionTestUtils.setField(activeSymbolChangesEventPublisher, "publishTimeoutMs", 1_000L);
  }

  @Test
  void publishLogsSuccessOnlyAfterBrokerAcknowledgement() {
    ActiveSymbolChanges event =
        new ActiveSymbolChanges(Set.of(TEST_ASSET_SYMBOL_AAPL), Set.of(TEST_ASSET_SYMBOL_MSFT));
    CompletableFuture<SendResult<String, ActiveSymbolChanges>> sendFuture =
        CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(ACTIVE_SYMBOL_CHANGES_TOPIC, event)).thenReturn(sendFuture);

    assertDoesNotThrow(() -> activeSymbolChangesEventPublisher.publish(event));

    verify(kafkaTemplate).send(ACTIVE_SYMBOL_CHANGES_TOPIC, event);
  }

  @Test
  void publishWrapsDeliveryFailuresWithEventContext() {
    ActiveSymbolChanges event =
        new ActiveSymbolChanges(Set.of(TEST_ASSET_SYMBOL_AAPL), Set.of(TEST_ASSET_SYMBOL_MSFT));
    CompletableFuture<SendResult<String, ActiveSymbolChanges>> sendFuture =
        new CompletableFuture<>();
    sendFuture.completeExceptionally(new IllegalStateException("broker unavailable"));
    when(kafkaTemplate.send(ACTIVE_SYMBOL_CHANGES_TOPIC, event)).thenReturn(sendFuture);

    EventPublishingException exception =
        assertThrows(
            EventPublishingException.class, () -> activeSymbolChangesEventPublisher.publish(event));

    assertEquals(ACTIVE_SYMBOL_CHANGES_TOPIC, exception.getMetadata().get("topic"));
    assertEquals("1", exception.getMetadata().get("addedSymbols"));
    assertEquals("1", exception.getMetadata().get("removedSymbols"));
    verify(kafkaTemplate).send(ACTIVE_SYMBOL_CHANGES_TOPIC, event);
  }
}
