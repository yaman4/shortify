package com.shortify.kafka;

import com.shortify.event.UrlAccessEvent;
import com.shortify.repository.UrlRepository;
import com.shortify.repository.UrlAccessAnalyticsRepository;
import com.shortify.model.UrlAccessAnalytics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka consumer for processing URL access events
 * Handles analytics updates asynchronously without blocking the redirect flow
 */
@Slf4j
@Component
public class UrlAccessEventConsumer {

    private final UrlRepository urlRepository;
    private final UrlAccessAnalyticsRepository analyticsRepository;

    public UrlAccessEventConsumer(UrlRepository urlRepository,
                                   UrlAccessAnalyticsRepository analyticsRepository) {
        this.urlRepository = urlRepository;
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Consumes URL access events and updates analytics
     * This method processes events asynchronously from Kafka
     *
     * @param event the URL access event to process
     */
    @KafkaListener(topics = "url-access-events", groupId = "shortify-analytics-group")
    @Transactional
    public void consumeUrlAccessEvent(UrlAccessEvent event) {
        try {
            log.info("Processing URL access event for shortCode: {} at timestamp: {}",
                    event.getShortCode(), event.getTimestamp());

            // IDEMPOTENCY: Check if this event was already processed (by unique event hash)
            String eventHash = event.getShortCode() + "-" + event.getTimestamp() + "-" + event.getUserAgent();
            boolean alreadyExists = analyticsRepository.existsByShortCodeAndAccessedAtAndUserAgent(
                    event.getShortCode(), event.getTimestamp(), event.getUserAgent());
            if (alreadyExists) {
                log.warn("Duplicate event detected, skipping analytics for eventHash: {}", eventHash);
                return;
            }

            // 1. Increment redirect count in UrlEntity (idempotent: only if not already incremented for this event)
            urlRepository.findByShortCode(event.getShortCode())
                    .ifPresentOrElse(
                            urlEntity -> {
                                urlEntity.setRedirectCount(urlEntity.getRedirectCount() + 1);
                                urlRepository.save(urlEntity);
                                log.debug("Updated redirect count for shortCode: {}", event.getShortCode());
                            },
                            () -> log.warn("URL entity not found for shortCode: {}", event.getShortCode())
                    );

            // 2. Store detailed analytics (idempotent)
            UrlAccessAnalytics analytics = new UrlAccessAnalytics();
            analytics.setShortCode(event.getShortCode());
            analytics.setOriginalUrl(event.getOriginalUrl());
            analytics.setAccessedAt(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            analytics.setIpAddress(event.getIp());
            analytics.setUserAgent(event.getUserAgent());
            analytics.setUrlId(event.getUrlId());

            UrlAccessAnalytics savedAnalytics = analyticsRepository.save(analytics);
            log.info("Stored analytics record with ID: {} for shortCode: {}", savedAnalytics.getId(), event.getShortCode());

        } catch (Exception e) {
            log.error("Error processing URL access event for shortCode: {}", event.getShortCode(), e);
            throw e; // Rethrow to trigger Kafka retry/DLQ
        }
    }
}
