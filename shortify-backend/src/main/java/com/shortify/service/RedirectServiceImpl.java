package com.shortify.service;

import com.shortify.event.UrlAccessEvent;
import com.shortify.kafka.UrlAccessEventProducer;
import com.shortify.model.UrlEntity;
import com.shortify.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;
    private final UrlAccessEventProducer urlAccessEventProducer;

    public RedirectServiceImpl(UrlRepository urlRepository, UrlAccessEventProducer urlAccessEventProducer) {
        this.urlRepository = urlRepository;
        this.urlAccessEventProducer = urlAccessEventProducer;
    }

    /**
     * Fetches the original URL by short code and publishes an async event for analytics.
     *
     * The increment of redirect count is now handled asynchronously by Kafka consumer.
     * This keeps the redirect response fast and decouples analytics from the redirect flow.
     *
     * @param shortCode the short URL code
     * @return original URL
     */
    @Transactional
    public String getLongUrlAndPublishEvent(String shortCode) {
        // Find the URL entity by short code
        UrlEntity urlEntity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // Extract request details for analytics
        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        // Create and publish event (non-blocking)
        UrlAccessEvent event = new UrlAccessEvent();
        event.setShortCode(shortCode);
        event.setOriginalUrl(urlEntity.getOriginalUrl());
        event.setUrlId(urlEntity.getId());
        event.setTimestamp(LocalDateTime.now());
        event.setIp(ipAddress);
        event.setUserAgent(userAgent);

        urlAccessEventProducer.publishUrlAccessEvent(event);
        log.debug("Published URL access event for shortCode: {} (async)", shortCode);

        // Return the original URL immediately (fast path)
        return urlEntity.getOriginalUrl();
    }

    @Override
    public String getLongUrlAndIncrementCount(String shortCode) {
        // Deprecated method - now uses async Kafka events instead
        return getLongUrlAndPublishEvent(shortCode);
    }

    @Override
    public String resolve(String shortCode) {
        return getLongUrlAndPublishEvent(shortCode);
    }

    /**
     * Extracts client IP address from HTTP request
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not extract client IP", e);
        }
        return "unknown";
    }

    /**
     * Extracts user agent from HTTP request
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not extract user agent", e);
        }
        return "unknown";
    }
}
