package com.shortify.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    // Local cache of buckets (per instance) for fast path
    private final Map<String, Bucket> localBucketCache = new ConcurrentHashMap<>();

    public RateLimitFilter(StringRedisTemplate redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        String bucketKey = "rate_limit:" + ip + ":" + path;

        // Get rate limit configuration for this endpoint
        long limitPerMinute = getLimitPerMinute(path);
        
        // Fast path: Check local in-memory bucket first (distributed across all requests on this instance)
        Bucket bucket = localBucketCache.computeIfAbsent(bucketKey, key -> 
            createBucket(limitPerMinute));

        // Try to consume 1 token from local bucket
        if (bucket.tryConsume(1)) {
            // Sync consumed token to Redis for cross-instance visibility
            syncToRedis(bucketKey, limitPerMinute);
            filterChain.doFilter(request, response);
        } else {
            // Local bucket exhausted - check if Redis has any capacity
            if (hasRedisCapacity(bucketKey, limitPerMinute)) {
                syncToRedis(bucketKey, limitPerMinute);
                filterChain.doFilter(request, response);
            } else {
                // Rate limit exceeded across instances
                log.debug("Rate limit exceeded for IP: {} path: {}", ip, path);
                response.setStatus(429);
                response.getWriter().write("Too many requests. Please try again later.");
            }
        }
    }

    /**
     * Creates a local Bucket4j bucket for fast in-memory rate limiting
     */
    private Bucket createBucket(long limitPerMinute) {
        Bandwidth limit = Bandwidth.classic(limitPerMinute, Refill.greedy(limitPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Syncs local bucket state to Redis for distributed coordination
     * Other instances can see this IP's consumption pattern
     */
    private void syncToRedis(String bucketKey, long limitPerMinute) {
        try {
            RAtomicLong counter = redissonClient.getAtomicLong(bucketKey);
            counter.incrementAndGet();
            // Expire after 1 minute (auto-cleanup)
            counter.expire(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to sync rate limit to Redis for key: {}", bucketKey, e);
            // Fail open - allow request if Redis unavailable
        }
    }

    /**
     * Checks if Redis indicates this IP has capacity across all instances
     * This provides distributed rate limiting awareness
     */
    private boolean hasRedisCapacity(String bucketKey, long limitPerMinute) {
        try {
            RAtomicLong counter = redissonClient.getAtomicLong(bucketKey);
            long currentCount = counter.get();
            
            if (currentCount < limitPerMinute) {
                // Still has capacity - allow and increment
                counter.incrementAndGet();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to check Redis rate limit for key: {}", bucketKey, e);
            // Fail open - allow request if Redis unavailable
            return true;
        }
    }

    /**
     * Determines rate limit based on endpoint
     */
    private long getLimitPerMinute(String path) {
        if (path.startsWith("/api/v1/shorten")) {
            return 10;  // 10 requests per minute for shorten
        } else {
            return 100; // 100 requests per minute for other endpoints
        }
    }
}
