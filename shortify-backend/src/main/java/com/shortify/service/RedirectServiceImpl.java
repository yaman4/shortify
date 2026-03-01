package com.shortify.service;

import com.shortify.model.UrlEntity;
import com.shortify.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;

    public RedirectServiceImpl(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Fetches the original URL by short code and increments redirect count.
     *
     * @param shortCode the short URL code
     * @return original URL
     */
    @Transactional
    public String getLongUrlAndIncrementCount(String shortCode) {
        // Find the URL entity by short code
        UrlEntity urlEntity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // Increment redirect count
        urlEntity.setRedirectCount(urlEntity.getRedirectCount() + 1);
        urlRepository.save(urlEntity);

        // Return the original URL
        return urlEntity.getOriginalUrl();
    }

    @Override
    public String resolve(String shortCode) {
        return getLongUrlAndIncrementCount(shortCode);
    }
}
