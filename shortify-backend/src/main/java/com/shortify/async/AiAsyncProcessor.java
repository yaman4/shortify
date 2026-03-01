package com.shortify.async;

import com.shortify.model.RiskLevel;
import com.shortify.repository.UrlRepository;
import com.shortify.service.AiService;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiAsyncProcessor {

    private final AiService aiService;
    private final UrlRepository urlRepository;

    public AiAsyncProcessor(@Lazy AiService aiService, UrlRepository urlRepository) {
        this.aiService = aiService;
        this.urlRepository = urlRepository;
    }

    @Async
    @Transactional
    public void processAsync(Long urlId, String longUrl) {
        RiskLevel risk = aiService.classifyUrl(longUrl);

        urlRepository.findById(urlId).ifPresent(entity -> {
            entity.setRiskLevel(risk);
            entity.setAiChecked(true);
            urlRepository.save(entity);
        });
    }
}
