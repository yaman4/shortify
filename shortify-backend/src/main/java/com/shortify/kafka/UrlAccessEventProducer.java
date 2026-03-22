package com.shortify.kafka;

import com.shortify.event.UrlAccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for publishing URL access events
 * Publishes events asynchronously to avoid blocking the redirect flow
 */
@Slf4j
@Component
public class UrlAccessEventProducer {

    private static final String TOPIC_NAME = "url-access-events";

    private final KafkaTemplate<String, UrlAccessEvent> kafkaTemplate;

    public UrlAccessEventProducer(KafkaTemplate<String, UrlAccessEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a URL access event to Kafka topic
     * This method is non-blocking and returns immediately
     * Partitioning logic: shortCode is used as the Kafka message key, ensuring all events for the same shortCode
     * go to the same partition. This preserves order and enables scalable processing.
     *
     * @param event the URL access event to publish
     */
    public void publishUrlAccessEvent(UrlAccessEvent event) {
        try {
            Message<UrlAccessEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
                    .setHeader("kafka_messageKey", event.getShortCode())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published URL access event for shortCode: {} at timestamp: {}",
                            event.getShortCode(), event.getTimestamp());
                } else {
                    log.error("Failed to publish URL access event for shortCode: {}",
                            event.getShortCode(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing URL access event for shortCode: {}", event.getShortCode(), e);
        }
    }
}
