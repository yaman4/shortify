package com.shortify.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event model for URL access analytics
 * Published to Kafka when a user accesses a shortened URL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlAccessEvent {

    @JsonProperty("short_code")
    private String shortCode;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("original_url")
    private String originalUrl;

    @JsonProperty("url_id")
    private Long urlId;
}

