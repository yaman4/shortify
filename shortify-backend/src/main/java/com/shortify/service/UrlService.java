package com.shortify.service;

import com.shortify.dto.ShortenRequest;
import com.shortify.dto.ShortenResponse;
import com.shortify.dto.UrlStatsResponse;
import com.shortify.model.UrlEntity;

import java.util.Optional;

public interface UrlService {
    ShortenResponse shortenUrl(ShortenRequest request);
    UrlStatsResponse getUrlStats(String shortCode);
    Optional<UrlEntity> getUrlEntityByShortCode(String shortCode);
    void saveUrlEntity(UrlEntity url);
}
