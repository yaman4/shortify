package com.shortify.service;

import com.shortify.model.RiskLevel;
import com.shortify.model.UrlEntity;

public interface AiService {
    RiskLevel classifyUrl(String url);
    boolean isMalicious(String url);
    String suggestAlias(String url);
    void analyzeUrl(UrlEntity url);
}
