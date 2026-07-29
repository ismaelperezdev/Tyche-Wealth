package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.LogConstants.ACTIVE_USERS_EVENT_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SYSTEM;

import com.tychewealth.kafka.events.ActiveUsersEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveUsersEventConsumer {

  private static final String ACTIVE_USERS_EVENT_ACTION = "[active-users-event]";

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
  }
}
