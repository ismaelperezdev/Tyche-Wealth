package com.tychewealth.config;

import java.time.Duration;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

  @Value("${app.kafka.dlt.suffix:-dlt}")
  private String deadLetterTopicSuffix;

  @Value("${app.kafka.listener.retry-interval-ms:1000}")
  private long retryIntervalMs;

  @Value("${app.kafka.listener.retry-attempts:2}")
  private long retryAttempts;

  @Value("${spring.kafka.producer.properties.delivery.timeout.ms:30000}")
  private long dltSendTimeoutMs;

  @Bean
  DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
      KafkaTemplate<Object, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (consumerRecord, exception) ->
                new TopicPartition(
                    consumerRecord.topic() + deadLetterTopicSuffix, consumerRecord.partition()));
    recoverer.setFailIfSendResultIsError(true);
    recoverer.setWaitForSendResultTimeout(Duration.ofMillis(dltSendTimeoutMs));
    return recoverer;
  }

  @Bean
  DefaultErrorHandler kafkaErrorHandler(
      DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
    DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(
            deadLetterPublishingRecoverer, new FixedBackOff(retryIntervalMs, retryAttempts));
    errorHandler.setCommitRecovered(true);
    return errorHandler;
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
      ConsumerFactory<Object, Object> consumerFactory,
      DefaultErrorHandler kafkaErrorHandler) {

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, consumerFactory);
    factory.setCommonErrorHandler(kafkaErrorHandler);
    return factory;
  }
}
