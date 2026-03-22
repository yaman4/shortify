package com.shortify.config;

import com.shortify.event.UrlAccessEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for URL access event streaming
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ============== Topic Configuration ==============

    /**
     * Define the url-access-events topic with 3 partitions and 1 replica
     */
    @Bean
    public NewTopic urlAccessEventsTopic() {
        return TopicBuilder.name("url-access-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ============== Producer Configuration ==============

    /**
     * Kafka producer factory configuration
     */
    @Bean
    public ProducerFactory<String, UrlAccessEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Acks configuration - wait for leader to write (good balance of speed and durability)
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");

        // Batch size for better throughput
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // Linger time to collect messages before sending
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        // Compression to reduce network bandwidth
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Retry policy
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for sending messages
     */
    @Bean
    public KafkaTemplate<String, UrlAccessEvent> kafkaTemplate(
            ProducerFactory<String, UrlAccessEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // ============== Consumer Configuration ==============

    /**
     * Kafka consumer factory configuration
     */
    @Bean
    public ConsumerFactory<String, UrlAccessEvent> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "shortify-analytics-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Auto-offset reset strategy
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Session timeout
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);

        // Heartbeat interval
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // Max poll interval
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        // Trusted packages for deserialization
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Enable auto-commit
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for concurrent message processing
     * Adds retry and DLQ (Dead Letter Queue) support
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, UrlAccessEvent>>
    kafkaListenerContainerFactory(ConsumerFactory<String, UrlAccessEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, UrlAccessEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Concurrency level - process messages from multiple partitions concurrently
        factory.setConcurrency(3);
        // Poll timeout
        factory.getContainerProperties().setPollTimeout(3000);

        // Retry: 3 attempts, then send to DLQ
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate(producerFactory()),
                        (record, ex) -> new TopicPartition("url-access-events.DLQ", record.partition())
                ),
                new FixedBackOff(1000L, 3) // 1 second interval, 3 attempts
        ));

        return factory;
    }
}
