package com.tychewealth.kafka.publishers;

import static com.tychewealth.constants.LogConstants.ACTIVE_USERS_EVENT_PUBLISH_FAILURE_MESSAGE;
import static com.tychewealth.constants.LogConstants.ACTIVE_USERS_EVENT_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SEND_ACTION;
import static com.tychewealth.constants.LogConstants.SYSTEM;

import com.tychewealth.error.exception.EventPublishingException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.kafka.events.ActiveUsersEvent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveUsersEventPublisher {

  private final KafkaTemplate<String, ActiveUsersEvent> kafkaTemplate;

  @Value("${app.kafka.topics.active-users}")
  private String activeUsersTopic;

  public ActiveUsersEventPublisher(KafkaTemplate<String, ActiveUsersEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(ActiveUsersEvent event) {
    try {
      kafkaTemplate.send(activeUsersTopic, event);
      log.info(
          REQUEST_SUCCESS + ACTIVE_USERS_EVENT_SUCCESS_CONTEXT,
          SYSTEM,
          SEND_ACTION,
          activeUsersTopic,
          event.userIds().size());
    } catch (RuntimeException error) {
      log.error(
          REQUEST_CONFLICT + com.tychewealth.constants.LogConstants.TOPIC,
          SYSTEM,
          SEND_ACTION,
          ACTIVE_USERS_EVENT_PUBLISH_FAILURE_MESSAGE,
          activeUsersTopic,
          error);
      throw EventPublishingException.of(
          ErrorDefinition.GENERIC_INTERNAL_ERROR,
          Map.of("topic", activeUsersTopic),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
