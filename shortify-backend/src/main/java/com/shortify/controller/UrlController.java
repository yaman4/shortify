package com.shortify.controller;

import com.shortify.dto.ShortenRequest;
import com.shortify.dto.ShortenResponse;
import com.shortify.dto.UrlStatsResponse;
import com.shortify.model.UrlEntity;
import com.shortify.service.AiService;
import com.shortify.service.UrlService;
import org.redisson.api.RRateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class UrlController {

    private final UrlService urlService;
    private final AiService aiService;
    private final RRateLimiter rateLimiter;

    public UrlController(UrlService urlService, AiService aiService, RRateLimiter rateLimiter) {
        this.urlService = urlService;
        this.aiService = aiService;
        this.rateLimiter = rateLimiter;
    }

    // Shorten URL endpoint
    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        // Rate limiting
        boolean allowed = rateLimiter.tryAcquire();
        if (!allowed) {
            return ResponseEntity.status(429).body("Too many requests. Try again later.");
        }

        // Malicious URL check
        if (aiService.isMalicious(request.getOriginalUrl())) {
            return ResponseEntity.status(400).body("Malicious URL detected!");
        }

        // Suggest alias if not provided
        if (request.getCustomAlias() == null || request.getCustomAlias().isEmpty()) {
            request.setCustomAlias(aiService.suggestAlias(request.getOriginalUrl()));
        }

        ShortenResponse response = urlService.shortenUrl(request);
        return ResponseEntity.ok(response);
    }

    // Get URL statistics endpoint
    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getUrlStats(@PathVariable String shortCode) {
        UrlStatsResponse stats = urlService.getUrlStats(shortCode);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/ai/check/{shortCode}")
    public ResponseEntity<String> triggerAi(@PathVariable String shortCode) {
        UrlEntity url = urlService.getUrlEntityByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        aiService.analyzeUrl(url);

        return ResponseEntity.ok("AI processing triggered for: " + shortCode);
    }
}
