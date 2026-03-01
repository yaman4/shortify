package com.shortify.dto;

import com.shortify.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlStatsResponse {
    private String shortUrl;
    private String originalUrl;
    private int redirectCount;
    private RiskLevel riskLevel;
    private boolean aiChecked;
}
