package com.shortify.scheduler;

import com.shortify.repository.UrlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ExpiredUrlCleanupJob {

    private final UrlRepository urlRepository;

    public ExpiredUrlCleanupJob(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    // Runs every hour
    @Scheduled(cron = "0 0 * * * *")
    public void cleanExpiredUrls() {
        urlRepository.findAll().forEach(url -> {
            if (url.getTtlInSeconds() > 0 &&
                    Instant.now().getEpochSecond() - url.getTtlInSeconds() > 0) {
                urlRepository.delete(url);
            }
        });
    }
}
