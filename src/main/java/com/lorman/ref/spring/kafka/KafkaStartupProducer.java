package com.lorman.ref.spring.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends a single message to topic "my.first.topic" right after the application starts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaStartupProducer {

    private static final String TOPIC = "my.first.topic";

    private final KafkaTemplate<String, KafkaMessageDTO> kafkaTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        KafkaMessageDTO payload = new KafkaMessageDTO("Hello from SpringLatestRef at startup");
        log.info("[KAFKA] Sending startup message to topic '{}'", TOPIC);
        kafkaTemplate.send(TOPIC, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to send startup message to '{}'", TOPIC, ex);
                    } else if (result != null) {
                        log.info("[KAFKA] Sent to '{}', partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.info("[KAFKA] Sent to '{}' (no metadata)", TOPIC);
                    }
                });
    }
}
