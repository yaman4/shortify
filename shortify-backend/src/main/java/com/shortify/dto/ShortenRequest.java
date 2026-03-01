package com.shortify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenRequest {
    @NotBlank
    private String originalUrl;

    private String customAlias;

    // Optional TTL in seconds
    private Long ttlInSeconds;
}
