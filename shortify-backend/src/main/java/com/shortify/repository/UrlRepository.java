package com.shortify.repository;

import com.shortify.model.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    // Custom method to delete expired URLs
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM urls u WHERE u.ttl_in_seconds IS NOT NULL AND (u.created_at + make_interval(secs => u.ttl_in_seconds)) <= :now", nativeQuery = true)
    void deleteExpiredUrls(Instant now);
}
