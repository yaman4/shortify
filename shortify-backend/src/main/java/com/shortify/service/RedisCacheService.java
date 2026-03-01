package com.shortify.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<String> get(String shortCode) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(shortCode)));
    }

    @Override
    public void put(String shortCode, String longUrl, Duration ttl) {
        redisTemplate.opsForValue().set(key(shortCode), longUrl, ttl);
    }

    private String key(String shortCode) {
        return "shortify:" + shortCode;
    }
}
