package com.shortify.service;

import com.shortify.model.UrlEntity;
import com.shortify.model.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    private final UrlService urlService;

    public AiServiceImpl(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Simulate AI processing on a URL.
     * Marks the URL as AI-checked and sets a risk level.
     */
    @Override
    public void analyzeUrl(UrlEntity url) {
        url.setAiChecked(true);
        url.setRiskLevel(classifyUrl(url.getOriginalUrl()));
        urlService.saveUrlEntity(url);
    }


    @Override
    public RiskLevel classifyUrl(String originalUrl) {
        if (originalUrl.toLowerCase().contains("malicious")) {
            return RiskLevel.MALICIOUS;
        }
        // Add more rules or AI logic here
        return RiskLevel.SAFE;
    }

    /**
     * Simple placeholder for malicious URL detection.
     */
    @Override
    public boolean isMalicious(String originalUrl) {
        // Basic logic: consider any URL containing "malicious" as unsafe
        return originalUrl.toLowerCase().contains("malicious");
    }

    /**
     * Suggests a custom alias for the URL.
     */
    @Override
    public String suggestAlias(String originalUrl) {
        return "alias" + (int)(Math.random() * 10000);
    }
}
