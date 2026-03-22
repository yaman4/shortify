package com.shortify.repository;

import com.shortify.model.UrlAccessAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing URL access analytics
 */
@Repository
public interface UrlAccessAnalyticsRepository extends JpaRepository<UrlAccessAnalytics, Long> {

    /**
     * Find all analytics records for a specific short code
     */
    List<UrlAccessAnalytics> findByShortCode(String shortCode);

    /**
     * Find all analytics records for a specific URL ID
     */
    List<UrlAccessAnalytics> findByUrlId(Long urlId);

    /**
     * Find analytics records for a short code within a date range
     */
    @Query("SELECT a FROM UrlAccessAnalytics a WHERE a.shortCode = :shortCode AND a.accessedAt BETWEEN :startTime AND :endTime")
    List<UrlAccessAnalytics> findAnalyticsByShortCodeAndDateRange(
            @Param("shortCode") String shortCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Count total accesses for a short code
     */
    long countByShortCode(String shortCode);

    /**
     * Count accesses for a short code within a date range
     */
    @Query("SELECT COUNT(a) FROM UrlAccessAnalytics a WHERE a.shortCode = :shortCode AND a.accessedAt BETWEEN :startTime AND :endTime")
    long countByShortCodeAndDateRange(
            @Param("shortCode") String shortCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Check if an analytics event already exists (for idempotency)
     */
    boolean existsByShortCodeAndAccessedAtAndUserAgent(String shortCode, java.time.LocalDateTime accessedAt, String userAgent);
}
