package com.shortify.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "urls")
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true)
    private String shortCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ttl_in_seconds")
    private Long ttlInSeconds;

    @Column(name = "redirect_count")
    private Integer redirectCount;

    @Column(name = "ai_checked")
    private Boolean aiChecked;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "custom_alias")
    private String customAlias;

    @PrePersist
    public void prePersist() {
        if (shortCode == null || shortCode.isEmpty()) {
            shortCode = RandomStringUtils.randomAlphanumeric(6);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (redirectCount == null) {
            redirectCount = 0;
        }
        if (aiChecked == null) {
            aiChecked = false;
        }
        if (riskLevel == null) {
            riskLevel = RiskLevel.UNKNOWN;
        }
    }
}
