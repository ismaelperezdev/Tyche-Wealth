package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.LogConstants.ACTIVE_SYMBOL_CHANGES_EVENT;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SYNC_ACTION;

import com.tychewealth.kafka.events.ActiveSymbolChanges;
import com.tychewealth.service.MarketSymbolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes active-symbol delta events and applies them to the market-symbol catalog.
 *
 * <p>When Kafka integration is enabled, listens to the configured active-symbol-changes topic and
 * delegates persistence and normalization to {@link MarketSymbolService}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveSymbolChangesEventConsumer {

  private final MarketSymbolService marketSymbolService;

  public ActiveSymbolChangesEventConsumer(MarketSymbolService marketSymbolService) {
    this.marketSymbolService = marketSymbolService;
  }

  @KafkaListener(
      topics = "${app.kafka.topics.active-symbol-changes}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void consume(
      ActiveSymbolChanges event, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    if (event == null) {
      log.info(
          REQUEST_SUCCESS + " topic={}",
          ACTIVE_SYMBOL_CHANGES_EVENT,
          SYNC_ACTION,
          0,
          receivedTopic);
      return;
    }

    marketSymbolService.applyChanges(event.addedSymbols(), event.removedSymbols());
    int changedSymbolsCount = event.addedSymbols().size() + event.removedSymbols().size();
    log.info(
        REQUEST_SUCCESS + " topic={}",
        ACTIVE_SYMBOL_CHANGES_EVENT,
        SYNC_ACTION,
        changedSymbolsCount,
        receivedTopic);
  }
}
