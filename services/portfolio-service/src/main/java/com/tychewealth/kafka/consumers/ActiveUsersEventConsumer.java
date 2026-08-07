package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.LogConstants.ACTIVE_USERS_EVENT_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SYSTEM;

import com.tychewealth.kafka.events.ActiveUsersEvent;
import com.tychewealth.service.activesymbol.ActiveSymbolService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes active-user events and refreshes the symbols tracked by the portfolio service.
 *
 * <p>When Kafka integration is enabled, listens to the configured active-users topic and delegates
 * the received user set to {@link ActiveSymbolService} for symbol synchronization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveUsersEventConsumer {

  private static final String ACTIVE_USERS_EVENT_ACTION = "[active-users-event]";

  private final ActiveSymbolService activeSymbolService;

  @KafkaListener(
      topics = "${app.kafka.topics.active-users}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void consume(
      ActiveUsersEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    int activeUsers = event == null || event.userIds() == null ? 0 : event.userIds().size();
    log.info(
        REQUEST_SUCCESS + ACTIVE_USERS_EVENT_SUCCESS_CONTEXT,
        SYSTEM,
        ACTIVE_USERS_EVENT_ACTION,
        receivedTopic,
        activeUsers);
    activeSymbolService.synchronizeSymbols(event == null ? Set.of() : event.userIds());
  }
}
