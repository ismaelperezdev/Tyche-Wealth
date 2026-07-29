package com.tychewealth.kafka.publishers;

import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SEND_ACTION;
import static com.tychewealth.constants.LogConstants.SYSTEM;

import com.tychewealth.error.exception.EventPublishingException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.kafka.events.ActiveSymbolChanges;
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

@Component
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveSymbolChangesEventPublisher {

  private final KafkaTemplate<String, ActiveSymbolChanges> kafkaTemplate;

  @Value("${app.kafka.topics.active-symbol-changes}")
  private String activeSymbolChangesTopic;

  @Value("${spring.kafka.producer.properties.delivery.timeout.ms:30000}")
  private long publishTimeoutMs;

  public ActiveSymbolChangesEventPublisher(
      KafkaTemplate<String, ActiveSymbolChanges> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(ActiveSymbolChanges event) {
    try {
      CompletableFuture<SendResult<String, ActiveSymbolChanges>> sendFuture =
          kafkaTemplate.send(activeSymbolChangesTopic, event);
      sendFuture.get(publishTimeoutMs, TimeUnit.MILLISECONDS);
      log.info(
          REQUEST_SUCCESS + " topic={} addedSymbols={} removedSymbols={}",
          SYSTEM,
          SEND_ACTION,
          activeSymbolChangesTopic,
          event.addedSymbols().size(),
          event.removedSymbols().size());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw publishFailure(event, error);
    } catch (ExecutionException | TimeoutException | RuntimeException error) {
      throw publishFailure(event, error);
    }
  }

  private EventPublishingException publishFailure(ActiveSymbolChanges event, Exception error) {
    log.error(
        REQUEST_CONFLICT + " topic={}",
        SYSTEM,
        SEND_ACTION,
        "active symbol changes event publish failed",
        activeSymbolChangesTopic,
        error);
    return EventPublishingException.of(
        ErrorDefinition.GENERIC_INTERNAL_ERROR,
        Map.of(
            "topic", activeSymbolChangesTopic,
            "addedSymbols", String.valueOf(event.addedSymbols().size()),
            "removedSymbols", String.valueOf(event.removedSymbols().size())),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
