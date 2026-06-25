package com.shortify.service;

import com.shortify.dto.ShortenRequest;
import com.shortify.dto.ShortenResponse;
import com.shortify.dto.UrlStatsResponse;
import com.shortify.model.UrlEntity;
import com.shortify.repository.UrlRepository;
import com.shortify.async.AiAsyncProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final AiAsyncProcessor aiAsyncProcessor;

    public UrlServiceImpl(UrlRepository urlRepository, AiAsyncProcessor aiAsyncProcessor) {
        this.urlRepository = urlRepository;
        this.aiAsyncProcessor = aiAsyncProcessor;
    }

    @Override
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request) {
        UrlEntity urlEntity = new UrlEntity();
        urlEntity.setOriginalUrl(request.getOriginalUrl());
        urlEntity.setTtlInSeconds(request.getTtlInSeconds());

        /**
         * Collision Prevention Strategy:
         * 
         * 1. Custom Alias Path (if provided):
         *    - Use the custom alias as-is
         *    - Unique constraint will prevent duplicates
         *    - User takes responsibility for uniqueness
         *
         * 2. Default Path (if no custom alias):
         *    - Leave shortCode null → triggers @PostPersist in UrlEntity
         *    - After DB assigns unique ID → convert ID to Base62
         *    - Guarantees NO collisions (each ID maps to unique shortCode)
         *    - Example: ID 238328 → shortCode "1000"
         */
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            urlEntity.setCustomAlias(request.getCustomAlias());
            urlEntity.setShortCode(request.getCustomAlias());
        }
        // else: shortCode remains null, will be generated in @PostPersist using Base62

        // Save entity (shortCode will be generated from ID if not provided)
        urlRepository.save(urlEntity);

        String shortUrl = "http://localhost:8080/" + urlEntity.getShortCode();

        return new ShortenResponse(shortUrl, urlEntity.getOriginalUrl(), urlEntity.getTtlInSeconds());
    }

    @Override
    public UrlStatsResponse getUrlStats(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .map(entity -> {
                    UrlStatsResponse stats = new UrlStatsResponse();
                    stats.setShortUrl(entity.getShortCode());
                    stats.setOriginalUrl(entity.getOriginalUrl());
                    stats.setRedirectCount(entity.getRedirectCount());
                    stats.setRiskLevel(entity.getRiskLevel());
                    stats.setAiChecked(entity.getAiChecked());
                    return stats;
                })
                .orElse(null);
    }

    @Override
    public Optional<UrlEntity> getUrlEntityByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode);
    }

    @Override
    public void saveUrlEntity(UrlEntity url) {
        urlRepository.save(url);
    }
}
