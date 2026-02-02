package com.lorman.ref.spring.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Enables Kafka infrastructure for this app when kafka.enabled=true.
 * By default (kafka.enabled=false) nothing related to Kafka is activated,
 * so tests and local runs without a broker are unaffected.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaFeatureConfig {
}
