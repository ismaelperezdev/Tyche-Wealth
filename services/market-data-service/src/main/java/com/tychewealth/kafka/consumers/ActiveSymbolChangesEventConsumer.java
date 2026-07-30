package com.tychewealth.kafka.consumers;

import static com.tychewealth.constants.LogConstants.ACTIVE_SYMBOL_CHANGES_EVENT;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SYNC_ACTION;

import com.tychewealth.kafka.events.ActiveSymbolChanges;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveSymbolChangesEventConsumer {

  @KafkaListener(
      topics = "${app.kafka.topics.active-symbol-changes}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void consume(
      ActiveSymbolChanges event, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    int changedSymbolsCount =
        event == null ? 0 : event.addedSymbols().size() + event.removedSymbols().size();
    log.info(
        REQUEST_SUCCESS + " topic={}",
        ACTIVE_SYMBOL_CHANGES_EVENT,
        SYNC_ACTION,
        changedSymbolsCount,
        receivedTopic);
  }
}
