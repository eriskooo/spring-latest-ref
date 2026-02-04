package com.lorman.ref.spring.kafka;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"my.first.topic"})
@TestPropertySource(properties = {
        "kafka.enabled=true",
        // nasmeruje Spring Kafka na embedded broker
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        // čítaj od začiatku, aby sme v teste zachytili aj správy poslané pred pripojením consumer-a
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DirtiesContext
class KafkaEmbeddedIntegrationTest {

    @Autowired
    private KafkaTemplate<String, KafkaMessageDTO> kafkaTemplate;

    @SpyBean
    private KafkaMessageConsumer consumer;

    @Test
    void produceAndConsume_withEmbeddedKafka_worksWithJsonDto() {
        String randomValue = "test-" + UUID.randomUUID();

        KafkaMessageDTO dto = new KafkaMessageDTO(randomValue);
        kafkaTemplate.send("my.first.topic", dto);

        ArgumentMatcher<KafkaMessageDTO> matchesValue = m -> m != null && randomValue.equals(m.getValue());
        verify(consumer, timeout(10_000)).onMessage(argThat(matchesValue));
    }
}
