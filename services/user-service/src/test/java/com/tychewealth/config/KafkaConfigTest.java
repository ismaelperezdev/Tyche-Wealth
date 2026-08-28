package com.tychewealth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

class KafkaConfigTest {

  @Test
  void shouldCreateActiveUsersTopicWithConfiguredPartitions() {
    NewTopic topic = new KafkaConfig().activeUsersTopic("active-users", 3, 2);

    assertEquals("active-users", topic.name());
    assertEquals(3, topic.numPartitions());
    assertEquals(2, topic.replicationFactor());
  }
}
