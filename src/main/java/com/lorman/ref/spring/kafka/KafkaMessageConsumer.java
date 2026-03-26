package com.lorman.ref.spring.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Simple Kafka consumer that listens on topic "my.first.topic" and logs the payload.
 *
 * @RetryableTopic auto-creates retry topics and a Dead Letter Topic (DLT):
 *   my.first.topic-retry-0, my.first.topic-retry-1, my.first.topic.DLT
 * Messages that fail all attempts land on the DLT for manual inspection/replay.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaMessageConsumer {

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "my.first.topic", groupId = "spring-latest-ref-group")
    public void onMessage(KafkaMessageDTO payload) {
        log.info("[KAFKA] Consumed from 'my.first.topic': {}", payload);
    }
}
