package com.tychewealth.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
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
  }

  @Test
  void shouldCreateErrorHandlerWithDeadLetterRecoverer() {
    @SuppressWarnings("unchecked")
    KafkaTemplate<Object, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);

    DeadLetterPublishingRecoverer recoverer =
        kafkaConfig.deadLetterPublishingRecoverer(kafkaTemplate);
    DefaultErrorHandler errorHandler = kafkaConfig.kafkaErrorHandler(recoverer);

    assertNotNull(recoverer);
    assertNotNull(errorHandler);
  }

  @Test
  void shouldAttachCommonErrorHandlerToKafkaListenerFactory() {
    @SuppressWarnings("unchecked")
    ConsumerFactory<Object, Object> consumerFactory = Mockito.mock(ConsumerFactory.class);
    ConcurrentKafkaListenerContainerFactoryConfigurer configurer =
        Mockito.mock(ConcurrentKafkaListenerContainerFactoryConfigurer.class);
    DefaultErrorHandler errorHandler =
        kafkaConfig.kafkaErrorHandler(
            kafkaConfig.deadLetterPublishingRecoverer(Mockito.mock(KafkaTemplate.class)));

    doAnswer(
            invocation -> {
              ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                  invocation.getArgument(0);
              factory.setConsumerFactory(consumerFactory);
              return null;
            })
        .when(configurer)
        .configure(
            Mockito.any(ConcurrentKafkaListenerContainerFactory.class),
            Mockito.eq(consumerFactory));

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        kafkaConfig.kafkaListenerContainerFactory(configurer, consumerFactory, errorHandler);

    CommonErrorHandler configuredErrorHandler =
        (CommonErrorHandler) ReflectionTestUtils.getField(factory, "commonErrorHandler");
    assertSame(errorHandler, configuredErrorHandler);
  }
}
