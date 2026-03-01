package com.shortify.config;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    private static final long REQUESTS_PER_MINUTE = 10; // example

    @Bean
    public RRateLimiter rateLimiter(RedissonClient redissonClient) {
        RRateLimiter limiter = redissonClient.getRateLimiter("shortify-limiter");
        limiter.trySetRate(RateType.OVERALL, REQUESTS_PER_MINUTE, 1, RateIntervalUnit.MINUTES);
        return limiter;
    }
}
