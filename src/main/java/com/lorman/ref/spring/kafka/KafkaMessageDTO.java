package com.lorman.ref.spring.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple data holder for Kafka messages to avoid sending plain strings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessageDTO {
    private String value;
}
