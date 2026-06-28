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

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;
    private final UrlAccessEventProducer urlAccessEventProducer;
    private final CacheService cacheService;

    public RedirectServiceImpl(UrlRepository urlRepository, UrlAccessEventProducer urlAccessEventProducer, CacheService cacheService) {
        this.urlRepository = urlRepository;
        this.urlAccessEventProducer = urlAccessEventProducer;
        this.cacheService = cacheService;
    }

    /**
     * Fetches the original URL by short code and publishes an async event for analytics.
     *
     * Caching strategy:
     * 1. Check Redis cache first (cache hit → return immediately)
     * 2. Cache miss → query database, populate cache with per-URL TTL, return URL
     * 3. TTL respects user-provided ttlInSeconds (or no expiration if null)
     *
     * The increment of redirect count is now handled asynchronously by Kafka consumer.
     * This keeps the redirect response fast and decouples analytics from the redirect flow.
     *
     * @param shortCode the short URL code
     * @return original URL
     */
    @Transactional
    public String getLongUrlAndPublishEvent(String shortCode) {
        // Try cache first (write-through strategy)
        var cachedUrl = cacheService.get(shortCode);
        if (cachedUrl.isPresent()) {
            log.debug("Cache hit for shortCode: {}", shortCode);
            publishAnalyticsEvent(shortCode, cachedUrl.get());
            return cachedUrl.get();
        }

        // Cache miss - fetch from database
        UrlEntity urlEntity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // Cache the URL with its configured TTL
        if (urlEntity.getTtlInSeconds() != null) {
            cacheService.put(shortCode, urlEntity.getOriginalUrl(), Duration.ofSeconds(urlEntity.getTtlInSeconds()));
            log.debug("Cached shortCode: {} with TTL: {} seconds", shortCode, urlEntity.getTtlInSeconds());
        } else {
            // No TTL provided - cache indefinitely (will only be evicted on manual deletion)
            cacheService.put(shortCode, urlEntity.getOriginalUrl(), Duration.ofDays(365));
            log.debug("Cached shortCode: {} with default long TTL", shortCode);
        }

        publishAnalyticsEvent(shortCode, urlEntity.getOriginalUrl());

        // Return the original URL immediately (fast path)
        return urlEntity.getOriginalUrl();
    }

    /**
     * Extracts request details and publishes URL access event for analytics
     */
    private void publishAnalyticsEvent(String shortCode, String originalUrl) {
        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        // Create and publish event (non-blocking)
        UrlAccessEvent event = new UrlAccessEvent();
        event.setShortCode(shortCode);
        event.setOriginalUrl(originalUrl);
        event.setTimestamp(LocalDateTime.now());
        event.setIp(ipAddress);
        event.setUserAgent(userAgent);

        urlAccessEventProducer.publishUrlAccessEvent(event);
        log.debug("Published URL access event for shortCode: {} (async)", shortCode);
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
