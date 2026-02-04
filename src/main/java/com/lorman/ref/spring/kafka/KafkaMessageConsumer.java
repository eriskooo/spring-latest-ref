package com.lorman.ref.spring.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Simple Kafka consumer that listens on topic "my.first.topic" and logs the payload.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaMessageConsumer {

    @KafkaListener(topics = "my.first.topic", groupId = "spring-latest-ref-group")
    public void onMessage(KafkaMessageDTO payload) {
        log.info("[KAFKA] Consumed from 'my.first.topic': {}", payload);
    }
}
