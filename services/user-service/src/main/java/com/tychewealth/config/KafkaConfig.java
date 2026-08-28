package com.tychewealth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

  @Bean
  NewTopic activeUsersTopic(
      @Value("${app.kafka.topics.active-users}") String topicName,
      @Value("${app.kafka.topic-partitions:3}") int partitions,
      @Value("${app.kafka.topic-replication-factor:1}") int replicationFactor) {
    return TopicBuilder.name(topicName).partitions(partitions).replicas(replicationFactor).build();
  }
}
