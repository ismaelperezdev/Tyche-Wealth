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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publishes active-user changes to Kafka for consumption by dependent services.
 *
 * <p>The event is sent to the configured active-users topic after a successful user snapshot.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveUsersEventPublisher {

  private static final String ACTIVE_USERS_TOPIC_KEY = "active-users";

  private final KafkaTemplate<String, ActiveUsersEvent> kafkaTemplate;

  @Value("${app.kafka.topics.active-users}")
  private String activeUsersTopic;

  @Value("${spring.kafka.producer.properties.delivery.timeout.ms:30000}")
  private long publishTimeoutMs;

  public ActiveUsersEventPublisher(KafkaTemplate<String, ActiveUsersEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(ActiveUsersEvent event) {
    try {
      CompletableFuture<SendResult<String, ActiveUsersEvent>> sendFuture =
          kafkaTemplate.send(activeUsersTopic, ACTIVE_USERS_TOPIC_KEY, event);
      sendFuture.get(publishTimeoutMs, TimeUnit.MILLISECONDS);
      log.info(
          REQUEST_SUCCESS + ACTIVE_USERS_EVENT_SUCCESS_CONTEXT,
          SYSTEM,
          SEND_ACTION,
          activeUsersTopic,
          event.userIds().size());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw publishFailure(event, error);
    } catch (ExecutionException | TimeoutException | RuntimeException error) {
      throw publishFailure(event, error);
    }
  }

  private EventPublishingException publishFailure(ActiveUsersEvent event, Exception error) {
    log.error(
        REQUEST_CONFLICT + com.tychewealth.constants.LogConstants.TOPIC,
        SYSTEM,
        SEND_ACTION,
        ACTIVE_USERS_EVENT_PUBLISH_FAILURE_MESSAGE,
        activeUsersTopic,
        error);
    return EventPublishingException.of(
        ErrorDefinition.GENERIC_INTERNAL_ERROR,
        Map.of("topic", activeUsersTopic, "activeUsers", String.valueOf(event.userIds().size())),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
