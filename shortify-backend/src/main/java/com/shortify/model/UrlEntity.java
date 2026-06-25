package com.shortify.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.shortify.util.Base62Encoder;

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
        // Don't generate shortCode here - wait for ID to be assigned in PostPersist
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

    @PostPersist
    public void postPersist() {
        // Generate shortCode from ID using Base62 encoding after persistence.
        // Collision Prevention Strategy:
        // - Each URL gets a unique auto-increment ID from database
        // - ID is encoded using Base62 (0-9, a-z, A-Z) = 62 chars
        // - No two IDs will ever produce same short code
        // - Guarantees uniqueness without retry logic
        // Examples: ID 1 → "1", ID 62 → "10", ID 3843 → "zzz", ID 238328 → "1000"
        
        if ((shortCode == null || shortCode.isEmpty()) && id != null) {
            // Only generate if not set (no custom alias provided)
            shortCode = new Base62Encoder().encode(id);
        } else if (customAlias == null || customAlias.isEmpty()) {
            // Fallback: if custom alias is empty but shortCode exists, encode ID
            if (id != null) {
                shortCode = new Base62Encoder().encode(id);
            }
        }
    }
}
