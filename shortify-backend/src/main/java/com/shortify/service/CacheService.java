package com.shortify.service;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {
    Optional<String> get(String shortCode);
    void put(String shortCode, String longUrl, Duration ttl);
}
