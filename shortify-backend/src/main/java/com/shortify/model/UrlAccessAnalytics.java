package com.shortify.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store detailed URL access analytics
 * Each record represents a single access to a shortened URL
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "url_access_analytics")
public class UrlAccessAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id")
    private Long urlId;

    @Column(name = "short_code", nullable = false)
    private String shortCode;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

