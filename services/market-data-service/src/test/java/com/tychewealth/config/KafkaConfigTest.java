package com.tychewealth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.util.ReflectionTestUtils;

class KafkaConfigTest {

  private KafkaConfig kafkaConfig;

  @BeforeEach
  void setUp() {
    kafkaConfig = new KafkaConfig();
    ReflectionTestUtils.setField(kafkaConfig, "deadLetterTopicSuffix", "-dlt");
    ReflectionTestUtils.setField(kafkaConfig, "retryIntervalMs", 1000L);
    ReflectionTestUtils.setField(kafkaConfig, "retryAttempts", 2L);
    ReflectionTestUtils.setField(kafkaConfig, "dltSendTimeoutMs", 30000L);
  }

  @Test
  void deadLetterPublishingRecovererRoutesToTopicWithSuffix() {
    @SuppressWarnings("unchecked")
    KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);

    DeadLetterPublishingRecoverer recoverer =
        kafkaConfig.deadLetterPublishingRecoverer(kafkaTemplate);

    @SuppressWarnings("unchecked")
    BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver =
        (BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition>)
            ReflectionTestUtils.getField(recoverer, "destinationResolver");

    TopicPartition topicPartition =
        destinationResolver.apply(
            new ConsumerRecord<>("active-symbol-changes", 3, 0L, "key", "value"),
            new IllegalStateException("boom"));

    assertNotNull(recoverer);
    assertNotNull(destinationResolver);
    assertEquals("active-symbol-changes-dlt", topicPartition.topic());
    assertEquals(3, topicPartition.partition());
    assertEquals(true, ReflectionTestUtils.getField(recoverer, "failIfSendResultIsError"));
    assertEquals(
        Duration.ofMillis(30000L),
        ReflectionTestUtils.getField(recoverer, "waitForSendResultTimeout"));
  }

  @Test
  void kafkaErrorHandlerCommitsRecoveredOffsets() {
    DeadLetterPublishingRecoverer recoverer = mock(DeadLetterPublishingRecoverer.class);

    DefaultErrorHandler errorHandler = kafkaConfig.kafkaErrorHandler(recoverer);

    assertNotNull(errorHandler);
    assertEquals(true, ReflectionTestUtils.getField(errorHandler, "commitRecovered"));
  }

  @Test
  void kafkaListenerContainerFactoryConfiguresFactoryAndSetsCommonErrorHandler() {
    ConcurrentKafkaListenerContainerFactoryConfigurer configurer =
        mock(ConcurrentKafkaListenerContainerFactoryConfigurer.class);
    @SuppressWarnings("unchecked")
    ConsumerFactory<Object, Object> consumerFactory = mock(ConsumerFactory.class);
    DefaultErrorHandler errorHandler = mock(DefaultErrorHandler.class);

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        kafkaConfig.kafkaListenerContainerFactory(configurer, consumerFactory, errorHandler);

    assertNotNull(factory);
    verify(configurer).configure(factory, consumerFactory);
    assertEquals(errorHandler, ReflectionTestUtils.getField(factory, "commonErrorHandler"));
  }
}
