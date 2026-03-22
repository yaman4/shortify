package com.shortify.kafka;

import com.shortify.event.UrlAccessEvent;
import com.shortify.model.UrlAccessAnalytics;
import com.shortify.model.UrlEntity;
import com.shortify.repository.UrlAccessAnalyticsRepository;
import com.shortify.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Kafka Integration Test Suite
 *
 * This unified test class contains all Kafka functionality tests:
 * - Producer tests (event creation and publishing)
 * - Consumer tests (event processing and database updates)
 * - End-to-end integration tests (complete workflow)
 *
 * Run all tests: ./mvnw test -Dtest=KafkaIntegrationTest
 * Run by nested class: ./mvnw test -Dtest=KafkaIntegrationTest\$ProducerTests
 * Run specific test: ./mvnw test -Dtest=KafkaIntegrationTest\$ProducerTests#testEventCreation
 */
@Slf4j
@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:29092",
        "port=29092"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:29092",
    "spring.kafka.producer.bootstrap-servers=localhost:29092",
    "spring.kafka.consumer.bootstrap-servers=localhost:29092",
    "spring.kafka.consumer.group-id=test-group",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class KafkaIntegrationTest {

    @Autowired
    private UrlAccessEventProducer urlAccessEventProducer;

    @Autowired
    private UrlAccessEventConsumer urlAccessEventConsumer;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UrlAccessAnalyticsRepository analyticsRepository;

    @Autowired
    private KafkaTemplate<String, UrlAccessEvent> kafkaTemplate;

    // ============== PRODUCER TESTS ==============

    @Nested
    @DisplayName("Producer Tests")
    class ProducerTests {

        /**
         * Test: UrlAccessEvent creation
         * Verifies: Event object can be created and populated
         */
        @Test
        @DisplayName("Should create UrlAccessEvent with all fields")
        public void testEventCreation() {
            // Arrange & Act
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("abc123");
            event.setOriginalUrl("https://example.com");
            event.setTimestamp(LocalDateTime.now());
            event.setIp("192.168.1.1");
            event.setUserAgent("Mozilla/5.0");
            event.setUrlId(1L);

            // Assert
            assertEquals("abc123", event.getShortCode());
            assertEquals("https://example.com", event.getOriginalUrl());
            assertEquals("192.168.1.1", event.getIp());
            assertEquals("Mozilla/5.0", event.getUserAgent());
            assertEquals(1L, event.getUrlId());
            assertNotNull(event.getTimestamp());

            log.info("Event creation test passed");
        }

        /**
         * Test: Producer publishes valid events
         * Verifies: publishUrlAccessEvent doesn't throw exceptions
         */
        @Test
        @DisplayName("Should publish valid events without exception")
        public void testPublishValidEvent() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("test456");
            event.setOriginalUrl("https://test.example.com");
            event.setTimestamp(LocalDateTime.now());
            event.setIp("10.0.0.1");
            event.setUserAgent("Safari/537.36");
            event.setUrlId(2L);

            // Act & Assert
            assertDoesNotThrow(() -> {
                urlAccessEventProducer.publishUrlAccessEvent(event);
                log.info("Valid event published successfully");
            });
        }

        /**
         * Test: Producer handles null optional fields
         * Verifies: Event with null optional fields doesn't crash
         */
        @Test
        @DisplayName("Should handle null optional fields gracefully")
        public void testHandleNullOptionalFields() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("xyz789");
            event.setOriginalUrl("https://another.example.com");
            event.setUrlId(3L);
            // IP and UserAgent are intentionally null

            // Act & Assert
            assertDoesNotThrow(() -> {
                urlAccessEventProducer.publishUrlAccessEvent(event);
                log.info("Null optional fields handled");
            });
        }

        /**
         * Test: Producer handles special characters in URLs
         * Verifies: URLs with special characters handled correctly
         */
        @Test
        @DisplayName("Should handle special characters in URLs")
        public void testSpecialCharactersInUrl() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("special123");
            event.setOriginalUrl("https://example.com/path?param1=value1&param2=value2&special=!@#$%");
            event.setTimestamp(LocalDateTime.now());
            event.setIp("8.8.8.8");
            event.setUserAgent("Chrome/120.0");
            event.setUrlId(5L);

            // Act & Assert
            assertDoesNotThrow(() -> {
                urlAccessEventProducer.publishUrlAccessEvent(event);
                log.info("Special characters in URL handled");
            });
        }

        /**
         * Test: Producer handles long User-Agent string
         * Verifies: Large user agent strings don't cause issues
         */
        @Test
        @DisplayName("Should handle long User-Agent strings")
        public void testLongUserAgent() {
            // Arrange
            String longUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0 " +
                    "Additional/Headers/For/Testing/Purposes";

            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("long-ua");
            event.setOriginalUrl("https://example.com");
            event.setTimestamp(LocalDateTime.now());
            event.setIp("203.0.113.1");
            event.setUserAgent(longUserAgent);
            event.setUrlId(6L);

            // Act & Assert
            assertDoesNotThrow(() -> {
                urlAccessEventProducer.publishUrlAccessEvent(event);
                log.info("Long User-Agent handled");
            });
        }

        /**
         * Test: Producer handles IPv6 addresses
         * Verifies: IPv6 addresses supported
         */
        @Test
        @DisplayName("Should handle IPv6 addresses")
        public void testIPv6Address() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("ipv6-test");
            event.setOriginalUrl("https://ipv6.example.com");
            event.setTimestamp(LocalDateTime.now());
            event.setIp("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
            event.setUserAgent("Mozilla/5.0");
            event.setUrlId(7L);

            // Act & Assert
            assertDoesNotThrow(() -> {
                urlAccessEventProducer.publishUrlAccessEvent(event);
                log.info("IPv6 address handled");
            });
        }

        /**
         * Test: Multiple rapid event publishes
         * Verifies: Producer can handle burst of events
         */
        @Test
        @DisplayName("Should handle multiple rapid event publishes")
        public void testMultipleRapidPublishes() {
            // Arrange
            int eventCount = 100;

            // Act & Assert
            assertDoesNotThrow(() -> {
                for (int i = 0; i < eventCount; i++) {
                    UrlAccessEvent event = new UrlAccessEvent();
                    event.setShortCode("rapid-" + i);
                    event.setOriginalUrl("https://rapid.example.com/" + i);
                    event.setTimestamp(LocalDateTime.now());
                    event.setIp("192.168.1." + (i % 256));
                    event.setUserAgent("Bot/1.0");
                    event.setUrlId((long) i);

                    urlAccessEventProducer.publishUrlAccessEvent(event);
                }
                log.info("{} rapid events published", eventCount);
            });
        }
    }

    // ============== CONSUMER TESTS ==============

    @Nested
    @DisplayName("Consumer Tests")
    class ConsumerTests {

        private UrlEntity testUrlEntity;

        @BeforeEach
        public void setUp() {
            analyticsRepository.deleteAll();
            urlRepository.deleteAll();

            testUrlEntity = new UrlEntity();
            testUrlEntity.setOriginalUrl("https://consumer-test.example.com");
            testUrlEntity.setShortCode("cons123");
            testUrlEntity.setRedirectCount(0);
            testUrlEntity.setAiChecked(false);
            testUrlEntity = urlRepository.save(testUrlEntity);
            urlRepository.flush(); // Ensure commit
        }

        /**
         * Test: Consumer directly processes event
         * Verifies: consumeUrlAccessEvent increments redirect count
         */
        @Test
        @DisplayName("Should increment redirect count on event processing")
        public void testConsumerIncrementsRedirectCount() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode(testUrlEntity.getShortCode());
            event.setOriginalUrl(testUrlEntity.getOriginalUrl());
            event.setUrlId(testUrlEntity.getId());
            event.setTimestamp(LocalDateTime.now());
            event.setIp("192.168.1.100");
            event.setUserAgent("Mozilla/5.0");

            // Act
            urlAccessEventConsumer.consumeUrlAccessEvent(event);

            // Assert
            Optional<UrlEntity> updated = urlRepository.findByShortCode(testUrlEntity.getShortCode());
            assertTrue(updated.isPresent());
            assertEquals(1, updated.get().getRedirectCount());

            log.info("Redirect count incremented");
        }

        /**
         * Test: Consumer stores all analytics fields
         * Verifies: UrlAccessAnalytics record created with all fields
         */
        @Test
        @DisplayName("Should store all analytics fields")
        public void testStoreAllAnalyticsFields() {
            // Arrange
            LocalDateTime accessTime = LocalDateTime.now();
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode(testUrlEntity.getShortCode());
            event.setOriginalUrl(testUrlEntity.getOriginalUrl());
            event.setUrlId(testUrlEntity.getId());
            event.setTimestamp(accessTime);
            event.setIp("10.0.0.5");
            event.setUserAgent("Safari/537.36");

            // Act
            urlAccessEventConsumer.consumeUrlAccessEvent(event);

            // Assert
            List<UrlAccessAnalytics> analyticsList = analyticsRepository
                    .findByShortCode(testUrlEntity.getShortCode());
            assertFalse(analyticsList.isEmpty());

            UrlAccessAnalytics analytics = analyticsList.getFirst();
            assertNotNull(analytics.getId());
            assertEquals(testUrlEntity.getShortCode(), analytics.getShortCode());
            assertEquals(testUrlEntity.getOriginalUrl(), analytics.getOriginalUrl());
            assertEquals("10.0.0.5", analytics.getIpAddress());
            assertEquals("Safari/537.36", analytics.getUserAgent());
            assertEquals(testUrlEntity.getId(), analytics.getUrlId());
            assertNotNull(analytics.getAccessedAt());
            assertNotNull(analytics.getCreatedAt());

            log.info("All analytics fields stored");
        }

        /**
         * Test: Consumer handles non-existent URL gracefully
         * Verifies: Consumer doesn't crash when URL doesn't exist
         */
        @Test
        @DisplayName("Should handle non-existent URL gracefully")
        public void testHandleNonExistentUrl() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode("nonexistent");
            event.setOriginalUrl("https://nonexistent.example.com");
            event.setUrlId(9999L);
            event.setTimestamp(LocalDateTime.now());
            event.setIp("192.168.1.100");
            event.setUserAgent("Mozilla/5.0");

            // Act & Assert
            assertDoesNotThrow(() -> {
                urlAccessEventConsumer.consumeUrlAccessEvent(event);
                log.info("Non-existent URL handled gracefully");
            });
        }

        /**
         * Test: Consumer processes events sequentially
         * Verifies: Events processed in order with correct increments
         */
        @Test
        @DisplayName("Should process events sequentially with correct increments")
        public void testSequentialEventProcessing() {
            // Act
            for (int i = 1; i <= 5; i++) {
                UrlAccessEvent event = new UrlAccessEvent();
                event.setShortCode(testUrlEntity.getShortCode());
                event.setOriginalUrl(testUrlEntity.getOriginalUrl());
                event.setUrlId(testUrlEntity.getId());
                event.setTimestamp(LocalDateTime.now());
                event.setIp("192.168.1." + (100 + i));
                event.setUserAgent("Request-" + i);

                urlAccessEventConsumer.consumeUrlAccessEvent(event);

                // Assert after each event
                Optional<UrlEntity> updated = urlRepository.findByShortCode(testUrlEntity.getShortCode());
                assertTrue(updated.isPresent());
                assertEquals(i, updated.get().getRedirectCount());
            }

            log.info("Sequential events processed");
        }

        /**
         * Test: Consumer creates separate analytics record per event
         * Verifies: Multiple analytics records created for multiple events
         */
        @Test
        @DisplayName("Should create separate analytics record per event")
        public void testMultipleAnalyticsRecords() {
            // Act
            int eventCount = 3;
            for (int i = 0; i < eventCount; i++) {
                UrlAccessEvent event = new UrlAccessEvent();
                event.setShortCode(testUrlEntity.getShortCode());
                event.setOriginalUrl(testUrlEntity.getOriginalUrl());
                event.setUrlId(testUrlEntity.getId());
                event.setTimestamp(LocalDateTime.now());
                event.setIp("192.168.1." + (50 + i));
                event.setUserAgent("Event-" + i);

                urlAccessEventConsumer.consumeUrlAccessEvent(event);
            }

            // Assert
            List<UrlAccessAnalytics> analyticsList = analyticsRepository
                    .findByShortCode(testUrlEntity.getShortCode());
            assertEquals(eventCount, analyticsList.size());

            log.info("{} analytics records created", eventCount);
        }

        /**
         * Test: Consumer processes duplicate events (at-least-once semantics)
         * Verifies: System allows duplicate events
         */
        @Test
        @DisplayName("Should handle duplicate events (at-least-once semantics)")
        public void testDuplicateEventProcessing() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode(testUrlEntity.getShortCode());
            event.setOriginalUrl(testUrlEntity.getOriginalUrl());
            event.setUrlId(testUrlEntity.getId());
            event.setTimestamp(LocalDateTime.now());
            event.setIp("192.168.1.100");
            event.setUserAgent("Mozilla/5.0");

            // Act: Process same event twice
            urlAccessEventConsumer.consumeUrlAccessEvent(event);
            urlAccessEventConsumer.consumeUrlAccessEvent(event);

            // Assert
            Optional<UrlEntity> updated = urlRepository.findByShortCode(testUrlEntity.getShortCode());
            assertTrue(updated.isPresent());
            assertEquals(2, updated.get().getRedirectCount());

            List<UrlAccessAnalytics> analyticsList = analyticsRepository
                    .findByShortCode(testUrlEntity.getShortCode());
            assertEquals(2, analyticsList.size());

            log.info("Duplicate events processed");
        }
    }

    // ============== END-TO-END INTEGRATION TESTS ==============

    @Nested
    @DisplayName("End-to-End Integration Tests")
    class EndToEndTests {

        private UrlEntity testUrlEntity;

        @BeforeEach
        public void setUp() {
            analyticsRepository.deleteAll();
            urlRepository.deleteAll();

            testUrlEntity = new UrlEntity();
            testUrlEntity.setOriginalUrl("https://e2e-test.example.com");
            testUrlEntity.setShortCode("e2etest");
            testUrlEntity.setRedirectCount(0);
            testUrlEntity.setAiChecked(false);
            testUrlEntity = urlRepository.save(testUrlEntity);
            urlRepository.flush(); // Ensure commit
        }

        /**
         * Test: Complete workflow - Producer to Consumer
         * Producer → Kafka Topic → Consumer → Database
         */
        @Test
        @DisplayName("Should complete full producer to consumer workflow")
        public void testCompleteWorkflow() {
            // Arrange
            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode(testUrlEntity.getShortCode());
            event.setOriginalUrl(testUrlEntity.getOriginalUrl());
            event.setUrlId(testUrlEntity.getId());
            event.setTimestamp(LocalDateTime.now());
            event.setIp("192.168.1.100");
            event.setUserAgent("Mozilla/5.0 TestBrowser/1.0");

            // Act
            urlAccessEventProducer.publishUrlAccessEvent(event);

            // Assert
            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<UrlEntity> updatedEntity = urlRepository.findByShortCode(testUrlEntity.getShortCode());
                    assertTrue(updatedEntity.isPresent());
                    assertEquals(1, updatedEntity.get().getRedirectCount());

                    List<UrlAccessAnalytics> analyticsList = analyticsRepository
                            .findByShortCode(testUrlEntity.getShortCode());
                    assertFalse(analyticsList.isEmpty());
                });

            log.info("Complete workflow passed");
        }

        /**
         * Test: High-volume event processing
         * Verifies system can handle bulk events without data loss
         */
        @Test
        @DisplayName("Should handle high-volume event processing")
        public void testHighVolumeProcessing() {
            // Arrange
            int eventCount = 50;

            // Act
            for (int i = 0; i < eventCount; i++) {
                UrlAccessEvent event = new UrlAccessEvent();
                event.setShortCode(testUrlEntity.getShortCode());
                event.setOriginalUrl(testUrlEntity.getOriginalUrl());
                event.setUrlId(testUrlEntity.getId());
                event.setTimestamp(LocalDateTime.now().plusSeconds(i));
                event.setIp("192.168.1." + (100 + (i % 100)));
                event.setUserAgent("HighVolume-Requester-" + i);

                urlAccessEventProducer.publishUrlAccessEvent(event);
            }

            // Assert
            await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<UrlEntity> updatedEntity = urlRepository.findByShortCode(testUrlEntity.getShortCode());
                    assertTrue(updatedEntity.isPresent());
                    assertEquals(eventCount, updatedEntity.get().getRedirectCount());

                    List<UrlAccessAnalytics> analyticsList = analyticsRepository
                            .findByShortCode(testUrlEntity.getShortCode());
                    assertEquals(eventCount, analyticsList.size());
                });

            log.info("High-volume processing passed ({} events)", eventCount);
        }

        /**
         * Test: Multiple URLs being tracked simultaneously
         * Verifies no cross-contamination between different URLs
         */
        @Test
        @DisplayName("Should track multiple URLs independently")
        public void testMultipleUrlsTracked() {
            // Arrange
            UrlEntity url1 = testUrlEntity;

            UrlEntity url2 = new UrlEntity();
            url2.setOriginalUrl("https://url2.example.com");
            url2.setShortCode("url2code");
            url2.setRedirectCount(0);
            url2 = urlRepository.save(url2);

            // Act
            for (int i = 0; i < 10; i++) {
                UrlAccessEvent event1 = new UrlAccessEvent();
                event1.setShortCode(url1.getShortCode());
                event1.setOriginalUrl(url1.getOriginalUrl());
                event1.setUrlId(url1.getId());
                event1.setTimestamp(LocalDateTime.now());
                event1.setIp("10.0.0.1");
                event1.setUserAgent("User-1-Request-" + i);
                urlAccessEventProducer.publishUrlAccessEvent(event1);

                UrlAccessEvent event2 = new UrlAccessEvent();
                event2.setShortCode(url2.getShortCode());
                event2.setOriginalUrl(url2.getOriginalUrl());
                event2.setUrlId(url2.getId());
                event2.setTimestamp(LocalDateTime.now());
                event2.setIp("10.0.0.2");
                event2.setUserAgent("User-2-Request-" + i);
                urlAccessEventProducer.publishUrlAccessEvent(event2);
            }

            // Fix: assign finalUrl before lambda and use it inside
            final UrlEntity finalUrl = url2;
            await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<UrlEntity> updated1 = urlRepository.findByShortCode(url1.getShortCode());
                    Optional<UrlEntity> updated2 = urlRepository.findByShortCode(finalUrl.getShortCode());

                    assertTrue(updated1.isPresent() && updated2.isPresent());
                    assertEquals(10, updated1.get().getRedirectCount());
                    assertEquals(10, updated2.get().getRedirectCount());
                });

            log.info("Multiple URLs tracked independently");
        }

        /**
         * Test: Concurrent event publishing
         * Verifies: System handles concurrent events without data loss
         */
        @Test
        public void testConcurrentEventPublishing() throws InterruptedException {
            // Arrange
            int threadCount = 10;
            int eventsPerThread = 5;
            int totalExpectedEvents = threadCount * eventsPerThread;

            CountDownLatch latch = new CountDownLatch(threadCount);

            // Act
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < eventsPerThread; i++) {
                            UrlAccessEvent event = new UrlAccessEvent();
                            event.setShortCode(testUrlEntity.getShortCode());
                            event.setOriginalUrl(testUrlEntity.getOriginalUrl());
                            event.setUrlId(testUrlEntity.getId());
                            event.setTimestamp(LocalDateTime.now());
                            event.setIp("192.168." + threadId + "." + i);
                            event.setUserAgent("Thread-" + threadId + "-Request-" + i);

                            urlAccessEventProducer.publishUrlAccessEvent(event);
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await(30, TimeUnit.SECONDS);
            Thread.sleep(4000);

            // Assert
            Optional<UrlEntity> updatedEntity = urlRepository.findByShortCode(testUrlEntity.getShortCode());
            assertTrue(updatedEntity.isPresent());
            assertEquals(totalExpectedEvents, updatedEntity.get().getRedirectCount());

            log.info("Concurrent event publishing passed ({} events)", totalExpectedEvents);
        }

        /**
         * Test: Analytics query by date range
         * Verifies: Date range queries work correctly
         */
        @Test
        public void testAnalyticsDateRangeQuery() throws InterruptedException {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusHours(1);
            LocalDateTime endTime = now.plusHours(1);

            UrlAccessEvent event = new UrlAccessEvent();
            event.setShortCode(testUrlEntity.getShortCode());
            event.setOriginalUrl(testUrlEntity.getOriginalUrl());
            event.setUrlId(testUrlEntity.getId());
            event.setTimestamp(now);
            event.setIp("192.168.1.100");
            event.setUserAgent("Mozilla/5.0");

            // Act
            urlAccessEventProducer.publishUrlAccessEvent(event);
            Thread.sleep(2000);

            // Assert
            long countInRange = analyticsRepository.countByShortCodeAndDateRange(
                    testUrlEntity.getShortCode(), startTime, endTime);
            assertEquals(1, countInRange);

            LocalDateTime pastStart = now.minusHours(3);
            LocalDateTime pastEnd = now.minusHours(2);
            long countOutOfRange = analyticsRepository.countByShortCodeAndDateRange(
                    testUrlEntity.getShortCode(), pastStart, pastEnd);
            assertEquals(0, countOutOfRange);

            log.info("Date range query test passed");
        }
    }
}

